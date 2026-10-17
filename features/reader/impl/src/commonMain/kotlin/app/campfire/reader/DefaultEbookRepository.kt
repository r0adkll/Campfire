// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.reader

import app.campfire.account.api.AccountManager
import app.campfire.account.api.TokenRefresher
import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.UserScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.core.model.LibraryItemId
import app.campfire.core.session.UserSession
import app.campfire.core.session.requireServerUrl
import app.campfire.core.session.requiredUserId
import app.campfire.libraries.api.LibraryItemRepository
import app.campfire.network.di.DownloadClient
import app.campfire.reader.api.Ebook
import app.campfire.reader.api.EbookDownload
import app.campfire.reader.api.EbookRepository
import app.campfire.user.api.MediaProgressRepository
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentLength
import io.ktor.utils.io.readAvailable
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

/**
 * Keeps ebook files on this device: in the cache for books that were only opened, and in the
 * download directory for books downloaded to read offline.
 *
 * The server only serves the whole file, which is fetched through the [DownloadClient] so it
 * streams to disk rather than through memory. That client carries no auth, so the account's
 * bearer token and extra headers are attached here, as the desktop offline downloader does.
 */
@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class)
@Inject
class DefaultEbookRepository(
  private val userSession: UserSession,
  private val libraryItemRepository: LibraryItemRepository,
  private val mediaProgressRepository: MediaProgressRepository,
  @DownloadClient private val client: HttpClient,
  private val accountManager: AccountManager,
  private val tokenRefresher: TokenRefresher,
  private val storage: EbookStorage,
  private val dispatcherProvider: DispatcherProvider,
  @ForScope(UserScope::class) private val coroutineScopeHolder: CoroutineScopeHolder,
) : EbookRepository {

  /** Downloads started or finished this session; the rest are read from the disk. */
  private val downloads = MutableStateFlow<Map<LibraryItemId, EbookDownload>>(emptyMap())

  override suspend fun open(itemId: LibraryItemId): Result<Ebook> = runCatching {
    val item = libraryItemRepository.getLibraryItem(itemId)
    val format = requireNotNull(item.media.ebookFormat) { "Item $itemId has no ebook" }
    val downloaded = Path(storage.downloadDirectory(), fileName(itemId, format))
    val cached = Path(storage.cacheDirectory(), fileName(itemId, format))
    val file = when {
      downloaded.hasContent() -> downloaded
      cached.hasContent() -> cached
      else -> cached.also { fetch(itemId, it) }
    }

    // A fresh fetch keeps whichever of this device's and the server's positions is newer, so one
    // saved by another client is picked up; offline, the last one saved here is used
    val progress = mediaProgressRepository.getProgress(itemId, fresh = true)
      ?: mediaProgressRepository.getProgress(itemId)
    Ebook(
      itemId = itemId,
      title = item.media.metadata.title.orEmpty(),
      author = item.media.metadata.authorName,
      format = format,
      filePath = file.toString(),
      location = progress?.ebookLocation,
      progress = progress?.ebookProgress,
    )
  }

  override suspend fun saveProgress(itemId: LibraryItemId, location: String, progress: Float): Result<Unit> =
    runCatching { mediaProgressRepository.updateEbookProgress(itemId, location, progress) }

  override fun observeDownload(itemId: LibraryItemId): Flow<EbookDownload> = downloads
    .onStart {
      if (itemId !in downloads.value) {
        val onDisk = withContext(dispatcherProvider.io) { downloadedFile(itemId) }
        val download = onDisk?.let { EbookDownload(EbookDownload.State.Downloaded, it.size(), it.size()) }
        downloads.update { if (itemId in it) it else it + (itemId to (download ?: EbookDownload.None)) }
      }
    }
    .map { it[itemId] ?: EbookDownload.None }
    .distinctUntilChanged()

  override fun observeDownloads(): Flow<Set<LibraryItemId>> = downloads
    .onStart {
      val onDisk = withContext(dispatcherProvider.io) { downloadedFiles() }
      downloads.update { current ->
        current + onDisk
          .filterKeys { it !in current }
          .mapValues { (_, file) -> EbookDownload(EbookDownload.State.Downloaded, file.size(), file.size()) }
      }
    }
    .map { downloads -> downloads.filterValues { it.state in KEPT_STATES }.keys }
    .distinctUntilChanged()

  override fun download(itemId: LibraryItemId) {
    if (downloads.value[itemId]?.state == EbookDownload.State.Downloading) return
    setDownload(itemId, EbookDownload(EbookDownload.State.Downloading))
    coroutineScopeHolder.get().launch {
      try {
        val item = libraryItemRepository.getLibraryItem(itemId)
        val format = requireNotNull(item.media.ebookFormat) { "Item $itemId has no ebook" }
        val target = Path(storage.downloadDirectory(), fileName(itemId, format))
        val cached = Path(storage.cacheDirectory(), fileName(itemId, format))
        withContext(dispatcherProvider.io) {
          // A book already opened is moved over rather than fetched again
          if (cached.hasContent()) {
            moveFile(cached, target)
          } else {
            // About every percent, rather than every chunk
            var reported = 0L
            fetch(itemId, target) { bytes, total ->
              val step = if (total > 0) total / 100 else PROGRESS_STEP_UNKNOWN_SIZE
              if (bytes - reported >= step) {
                reported = bytes
                setDownload(itemId, EbookDownload(EbookDownload.State.Downloading, bytes, total))
              }
            }
          }
        }
        val size = target.size()
        setDownload(itemId, EbookDownload(EbookDownload.State.Downloaded, size, size))
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        bark(LogPriority.WARN, throwable = e) { "Couldn't download the ebook for $itemId" }
        setDownload(itemId, EbookDownload(EbookDownload.State.Failed))
      }
    }
  }

  override fun deleteDownload(itemId: LibraryItemId) {
    setDownload(itemId, EbookDownload.None)
    coroutineScopeHolder.get().launch(dispatcherProvider.io) {
      deleteFilesIn(storage.downloadDirectory(), itemId)
    }
  }

  override suspend fun deleteFiles(itemId: LibraryItemId) {
    setDownload(itemId, EbookDownload.None)
    withContext(dispatcherProvider.io) {
      deleteFilesIn(storage.downloadDirectory(), itemId)
      deleteFilesIn(storage.cacheDirectory(), itemId)
    }
  }

  private fun setDownload(itemId: LibraryItemId, download: EbookDownload) {
    downloads.update { it + (itemId to download) }
  }

  private fun downloadedFile(itemId: LibraryItemId): Path? = filesIn(storage.downloadDirectory(), itemId)
    .firstOrNull { it.hasContent() }

  /** Every whole downloaded book, by item; a `.part` file is one cut short. */
  private fun downloadedFiles(): Map<LibraryItemId, Path> {
    val dir = Path(storage.downloadDirectory())
    if (!SystemFileSystem.exists(dir)) return emptyMap()
    return SystemFileSystem.list(dir)
      .filter { !it.name.endsWith(".part") && it.hasContent() }
      .associateBy { it.name.substringBefore('.') }
  }

  private fun deleteFilesIn(directory: String, itemId: LibraryItemId) {
    filesIn(directory, itemId).forEach { SystemFileSystem.delete(it, mustExist = false) }
  }

  private fun filesIn(directory: String, itemId: LibraryItemId): List<Path> {
    val dir = Path(directory)
    if (!SystemFileSystem.exists(dir)) return emptyList()
    return SystemFileSystem.list(dir).filter { it.name.startsWith("$itemId.") }
  }

  /**
   * Fetch [itemId]'s ebook to [file], through a `.part` file so a broken download never leaves a
   * file behind that looks whole. [onProgress] gets the bytes so far and the size, or -1.
   */
  private suspend fun fetch(
    itemId: LibraryItemId,
    file: Path,
    onProgress: (bytes: Long, total: Long) -> Unit = { _, _ -> },
  ) = withContext(dispatcherProvider.io) {
    val userId = userSession.requiredUserId
    val serverUrl = userSession.requireServerUrl
    val url = "${serverUrl.trimEnd('/')}/api/items/$itemId/ebook"
    SystemFileSystem.createDirectories(file.parent!!)
    val partFile = Path("$file.part")

    val token = accountManager.getToken(userId)?.accessToken
    if (!fetch(url, userId, token, partFile, onProgress)) {
      val refreshed = tokenRefresher.refresh(userId, serverUrl, token)?.accessToken
        ?: throw IOException("Ebook download unauthorized and the token could not be refreshed")
      if (!fetch(url, userId, refreshed, partFile, onProgress)) {
        throw IOException("Ebook download unauthorized after a token refresh")
      }
    }
    SystemFileSystem.atomicMove(partFile, file)
    bark { "Downloaded ebook for $itemId to $file" }
  }

  /** Returns false when the server rejected [token] as unauthorized, true once the file is on disk. */
  private suspend fun fetch(
    url: String,
    userId: String,
    token: String?,
    partFile: Path,
    onProgress: (bytes: Long, total: Long) -> Unit,
  ): Boolean {
    val extraHeaders = accountManager.getExtraHeaders(userId).orEmpty()
    return client.prepareGet(url) {
      if (!token.isNullOrEmpty()) header(HttpHeaders.Authorization, "Bearer $token")
      extraHeaders.forEach { (name, value) -> header(name, value) }
    }.execute { response ->
      when (response.status) {
        HttpStatusCode.OK -> {
          val total = response.contentLength() ?: -1L
          val body = response.bodyAsChannel()
          SystemFileSystem.sink(partFile).buffered().use { sink ->
            val buffer = ByteArray(BUFFER_SIZE)
            var bytes = 0L
            while (true) {
              val read = body.readAvailable(buffer)
              if (read < 0) break
              sink.write(buffer, 0, read)
              bytes += read
              onProgress(bytes, total)
            }
          }
          true
        }
        HttpStatusCode.Unauthorized -> false
        else -> throw IOException("Ebook download failed: HTTP ${response.status.value}")
      }
    }
  }
}

private fun fileName(itemId: LibraryItemId, format: String) = "$itemId.$format"

private fun Path.size(): Long = SystemFileSystem.metadataOrNull(this)?.size ?: 0L

private fun Path.hasContent(): Boolean = size() > 0L

/** Move [from] to [to], copying where they're on different file systems (desktop's temp dir). */
private fun moveFile(from: Path, to: Path) {
  SystemFileSystem.createDirectories(to.parent!!)
  try {
    SystemFileSystem.atomicMove(from, to)
  } catch (_: IOException) {
    SystemFileSystem.source(from).buffered().use { source ->
      SystemFileSystem.sink(to).buffered().use { sink -> source.transferTo(sink) }
    }
    SystemFileSystem.delete(from)
  }
}

/** The states of a download that's kept, or on its way. */
private val KEPT_STATES = setOf(EbookDownload.State.Downloading, EbookDownload.State.Downloaded)

private const val BUFFER_SIZE = 64 * 1024
private const val PROGRESS_STEP_UNKNOWN_SIZE = 256 * 1024L
