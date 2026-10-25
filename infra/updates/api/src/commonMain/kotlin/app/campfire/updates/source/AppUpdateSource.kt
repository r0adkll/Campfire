// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.updates.source

import androidx.compose.runtime.Composable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Implementations for this will live in the respective app platform modules
 */
interface AppUpdateSource {

  /**
   * Emits whenever the update state may have changed outside of direct user action,
   * e.g. a background download progressing or developer tooling manipulating faked
   * update state. Observers should re-query [getAvailableUpdate] on each emission.
   */
  fun changes(): Flow<Unit> = emptyFlow()

  /**
   * Whether or not this build supports in-app updates at all. When false, no update
   * UI should be rendered.
   */
  val isSupported: Boolean

  /**
   * The least time between update checks made because the app returned to the foreground.
   * Checks after [changes] or a closed update sheet aren't held to it.
   */
  val checkInterval: Duration
    get() = 1.hours

  /**
   * The source's own update UI, shown in place of Campfire's update sheet, or null to use
   * Campfire's sheet driven by [installUpdate].
   */
  val updateSheet: AppUpdateSheet?
    get() = null

  /** The release channel this build follows, e.g. "beta" or "pr/1234", where the source has them. */
  val channel: String?
    get() = null

  /**
   * Get the latest available update
   *
   * @throws AppUpdateCheckException when the source couldn't be asked, so a failed check isn't
   * mistaken for being up to date.
   */
  suspend fun getAvailableUpdate(): AppUpdate?

  /**
   * Offers the build [buildId], which a build page's Open in App asked for, in the update sheet,
   * whatever channel this build follows. The offer stands until [closeOpenedBuild], so update
   * checks in the meantime don't replace it. False where the source has no builds to open.
   *
   * @throws AppUpdateCheckException when the source couldn't be asked about the build.
   */
  suspend fun openBuild(buildId: String): Boolean = false

  /** Lets update checks replace the build [openBuild] offered again, once its sheet closes. */
  fun closeOpenedBuild() = Unit

  /**
   * Kick of the update installation process
   */
  suspend fun installUpdate(): Flow<AppUpdateProgress>
}

/**
 * An update UI owned by an [AppUpdateSource], such as Drop's Update sheet.
 */
interface AppUpdateSheet {

  @Composable
  fun Content(onDismiss: () -> Unit)
}

data class AppUpdate(
  val versionName: String,
  val versionCode: Long,
  val releaseNotes: String? = null,
  /** A one-line summary of what changed, shown in place of [versionName] where there is one. */
  val headline: String? = null,
  val status: Status = Status.Available,
  /** The download's size, where the source knows it. */
  val sizeBytes: Long? = null,
  /** When the build was published, where the source knows it. */
  val publishedAt: Instant? = null,
  /** The release channel the build comes from, where the source has them. */
  val channel: String? = null,
  /** The pull request the build was made from, for pull request builds. */
  val pullRequest: Int? = null,
) {

  sealed interface Status {
    /** Ready to download and install over the installed app. */
    data object Available : Status

    /** The installed build's channel ended (its pull request closed); this switches to main. */
    data object ChannelEnded : Status

    /** Installing it means uninstalling first (a lower version code or a different signer). */
    data object ReinstallRequired : Status

    data class Downloading(val progress: Float) : Status

    data object ReadyToInstall : Status

    data object Installing : Status

    data object Failed : Status
  }
}

/** An update check that couldn't reach the [AppUpdateSource]'s server. */
class AppUpdateCheckException(message: String, cause: Throwable? = null) : Exception(message, cause)

data class AppUpdateProgress(
  val bytes: Long,
  val totalBytes: Long,
  val status: Status,
) {

  val progress: Float
    get() = if (bytes == -1L || totalBytes == -1L) 0f else bytes.toFloat() / totalBytes.toFloat()

  enum class Status {
    Pending,
    Downloading,
    Downloaded,
    Failed,
    Canceled,
  }
}
