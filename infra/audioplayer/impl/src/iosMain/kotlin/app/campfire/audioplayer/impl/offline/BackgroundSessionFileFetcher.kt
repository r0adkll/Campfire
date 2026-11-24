// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.offline

import app.campfire.account.api.AccountManager
import app.campfire.account.api.TokenRefresher
import app.campfire.audioplayer.impl.offline.BackgroundDownloadSession.HTTP_OK
import app.campfire.audioplayer.impl.offline.BackgroundDownloadSession.HTTP_PARTIAL_CONTENT
import app.campfire.audioplayer.impl.offline.BackgroundDownloadSession.STATUS_NOT_SAVED
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSURL
import platform.Foundation.setValue

/**
 * [FileFetcher] over the [BackgroundDownloadSession], so a file keeps downloading while the app is
 * suspended. Like desktop's `HttpFileFetcher`, it resumes a part file with a `Range` request and
 * attaches the user's credentials itself (bearer token plus the account's extra headers); a `401`
 * goes through [TokenRefresher], which lets the user client rotate the token, then retries once.
 *
 * - A transfer still running from an earlier launch, or started ahead of its turn by [prepare], is
 *   picked up rather than started over.
 * - An interrupted transfer carries on from its [SavedResumeData]. That replays the original
 *   request, credentials included, so one resumed after its token expired is refused, and the file
 *   starts again from its part file instead.
 * - [prepare] starts a download's later files while the app is in the foreground: iOS defers
 *   transfers created while the app is in the background until it's opened again.
 * - Stopping a fetch stops the rest of its download's transfers too, keeping their resume data.
 */
internal class BackgroundSessionFileFetcher(
  private val accountManager: AccountManager,
  private val tokenRefresher: TokenRefresher,
  private val root: Path,
  /** False while downloads must wait for Wi-Fi, so iOS holds transfers off mobile data while the app is suspended. */
  private val allowsCellularAccess: () -> Boolean,
) : FileFetcher {

  override suspend fun fetch(request: FetchRequest, partFile: Path, onProgress: (Long, Long) -> Unit) {
    val relativePath = partFile.relativePath()
    stoppingDownloadOnCancel(partFile) { fetchInto(request, partFile, relativePath, onProgress) }
  }

  override suspend fun prepare(upcoming: List<PendingFetch>) {
    if (upcoming.isEmpty()) return
    stoppingDownloadOnCancel(upcoming.first().partFile) { startEarly(upcoming) }
  }

  override suspend fun discard(partFiles: List<Path>) {
    if (partFiles.isEmpty()) return
    BackgroundDownloadSession.discardAll(partFiles.first().directory())
    partFiles.forEach { discardResumeData(it) }
  }

  private suspend fun discardResumeData(partFile: Path) {
    SavedResumeData.take(partFile)?.let { BackgroundDownloadSession.discardResumeData(it.data) }
  }

  /** Runs [block]; if it's cancelled, stops every transfer of [partFile]'s download, keeping their resume data. */
  private suspend fun stoppingDownloadOnCancel(partFile: Path, block: suspend () -> Unit) {
    try {
      block()
    } catch (e: CancellationException) {
      withContext(NonCancellable) { BackgroundDownloadSession.stopAll(partFile.directory()) }
      throw e
    }
  }

  private suspend fun startEarly(upcoming: List<PendingFetch>) {
    val running = BackgroundDownloadSession.unfinishedTasks().mapTo(HashSet()) { (_, tag) -> tag.relativePath }
    val pending = upcoming.filter { (request, part) -> part.relativePath() !in running && !part.isFilled(request) }
    if (pending.isEmpty()) return
    val token = accountManager.getToken(pending.first().request.userId)?.accessToken
    for ((request, part) in pending) {
      val relativePath = part.relativePath()
      val saved = SavedResumeData.take(part)
      val task = if (saved != null) {
        BackgroundDownloadSession.resumedTask(saved.data, TransferTag(relativePath, saved.offset))
      } else {
        val offset = part.length()
        BackgroundDownloadSession.newTask(urlRequest(request, offset, token), TransferTag(relativePath, offset))
      }
      BackgroundDownloadSession.start(task)
    }
  }

  private suspend fun fetchInto(
    request: FetchRequest,
    partFile: Path,
    relativePath: String,
    onProgress: (Long, Long) -> Unit,
  ) {
    val running = BackgroundDownloadSession.runningTask(relativePath)
    if (running != null) {
      val (task, tag) = running
      val outcome = BackgroundDownloadSession.await(task, onProgress)
      if (finish(request, partFile, tag.offset, outcome, onProgress)) return
    } else if (partFile.isFilled(request)) {
      // Finished ahead of its turn
      onProgress(request.expectedBytes, request.expectedBytes)
      return
    }

    val saved = SavedResumeData.take(partFile)
    if (saved != null) {
      onProgress(partFile.length(), request.expectedBytes)
      val task = BackgroundDownloadSession.resumedTask(saved.data, TransferTag(relativePath, saved.offset))
      if (finish(request, partFile, saved.offset, BackgroundDownloadSession.await(task, onProgress), onProgress)) return
    }

    val token = accountManager.getToken(request.userId)?.accessToken
    if (transfer(request, partFile, relativePath, token, onProgress)) return

    val refreshed = tokenRefresher.refresh(request.userId, request.serverUrl, token)?.accessToken
      ?: throw PermanentFetchException("Download rejected as unauthorized and the token could not be refreshed")
    if (!transfer(request, partFile, relativePath, refreshed, onProgress)) {
      throw PermanentFetchException("Download rejected as unauthorized after a token refresh")
    }
  }

  /** Returns false when the server rejected [token] as unauthorized, true once the file is complete. */
  private suspend fun transfer(
    request: FetchRequest,
    partFile: Path,
    relativePath: String,
    token: String?,
    onProgress: (Long, Long) -> Unit,
  ): Boolean {
    discardResumeData(partFile)
    val offset = partFile.length()
    onProgress(offset, request.expectedBytes)
    val task = BackgroundDownloadSession.newTask(urlRequest(request, offset, token), TransferTag(relativePath, offset))
    return finish(request, partFile, offset, BackgroundDownloadSession.await(task, onProgress), onProgress)
  }

  private suspend fun urlRequest(request: FetchRequest, offset: Long, token: String?): NSMutableURLRequest {
    val url = NSURL.URLWithString(request.url) ?: throw PermanentFetchException("Malformed download URL")
    return NSMutableURLRequest(uRL = url).apply {
      if (!token.isNullOrEmpty()) setValue("Bearer $token", forHTTPHeaderField = "Authorization")
      accountManager.getExtraHeaders(request.userId).orEmpty().forEach { (name, value) ->
        setValue(value, forHTTPHeaderField = name)
      }
      if (offset > 0) setValue("bytes=$offset-", forHTTPHeaderField = "Range")
      setAllowsCellularAccess(this@BackgroundSessionFileFetcher.allowsCellularAccess())
    }
  }

  /** Returns false when the server rejected the credentials; throws when the file isn't complete. */
  private fun finish(
    request: FetchRequest,
    partFile: Path,
    offset: Long,
    outcome: TransferOutcome,
    onProgress: (Long, Long) -> Unit,
  ): Boolean {
    val (status, totalBytes) = when (outcome) {
      is TransferOutcome.Failed -> throw IOException("Download interrupted: ${outcome.message}")
      is TransferOutcome.Finished -> outcome
    }
    val total = totalBytes.takeIf { it > 0 } ?: request.expectedBytes
    when (status) {
      HTTP_OK, HTTP_PARTIAL_CONTENT -> {
        val written = partFile.length()
        if (total > 0 && written != total) throw IOException("Download ended at $written of $total bytes")
        onProgress(written, written)
      }
      HTTP_RANGE_NOT_SATISFIABLE -> {
        // The part file already holds every byte (an earlier transfer finished it), or it's longer
        // than the file now on the server — start that one over.
        if (total <= 0 || offset != total) {
          SystemFileSystem.delete(partFile, mustExist = false)
          throw IOException("Partial download no longer matches the remote file")
        }
        onProgress(total, total)
      }
      HTTP_UNAUTHORIZED -> return false
      HTTP_FORBIDDEN, HTTP_NOT_FOUND -> throw PermanentFetchException("HTTP $status")
      STATUS_NOT_SAVED -> throw IOException("Unable to save the downloaded file")
      else -> throw IOException("HTTP $status")
    }
    return true
  }

  /**
   * Whether the part file already holds the whole file. Bodies only reach part files once their
   * transfer finishes, so a full one is a finished one.
   */
  private fun Path.isFilled(request: FetchRequest): Boolean =
    request.expectedBytes > 0 && length() == request.expectedBytes

  /** The download's directory, relative to the downloads directory. */
  private fun Path.directory(): String = relativePath().substringBeforeLast('/')

  private fun Path.relativePath(): String {
    val prefix = "$root/"
    val path = toString()
    require(path.startsWith(prefix)) { "$path is outside the downloads directory" }
    return path.removePrefix(prefix)
  }

  private companion object {
    const val HTTP_UNAUTHORIZED = 401
    const val HTTP_FORBIDDEN = 403
    const val HTTP_NOT_FOUND = 404
    const val HTTP_RANGE_NOT_SATISFIABLE = 416
  }
}
