// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.android.updates

import android.app.Activity
import android.app.Application
import app.campfire.core.di.AppScope
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.updates.source.AppUpdate
import app.campfire.updates.source.AppUpdateProgress
import app.campfire.updates.source.AppUpdateProgress.Status
import app.campfire.updates.source.AppUpdateSource
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.requestAppUpdateInfo
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

/**
 * [AppUpdateSource] backed by Google Play's in-app updates API for Play-distributed
 * production builds, using the flexible update flow: Play shows its confirmation dialog
 * (via [PlayUpdateFlowLauncher]), the update downloads in the background while the app
 * stays usable, and installation hands off to the platform installer once downloaded.
 */
@ContributesBinding(AppScope::class, replaces = [NoOpUpdateSource::class])
@Inject
class GooglePlayAppUpdateSource(
  private val application: Application,
  private val updateFlowLauncher: PlayUpdateFlowLauncher,
) : AppUpdateSource {

  private val updateManager by lazy { AppUpdateManagerFactory.create(application) }

  override val isSupported: Boolean = true

  // Play updates are delivered to every install; there is no tester sign-in.
  override fun isSignedIn(): Boolean = true

  override suspend fun signIn() = Unit

  override suspend fun isUpdateAvailable(): Boolean = getAvailableUpdate() != null

  override suspend fun getAvailableUpdate(): AppUpdate? {
    val info = try {
      updateManager.requestAppUpdateInfo()
    } catch (e: Exception) {
      bark(LogPriority.WARN, throwable = e) { "Unable to check Google Play for an app update" }
      return null
    }
    if (info.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE) return null
    if (!info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) return null

    val versionCode = info.availableVersionCode()
    return AppUpdate(
      versionName = versionCode.toVersionName(),
      versionCode = versionCode.toLong(),
    )
  }

  override suspend fun installUpdate(): Flow<AppUpdateProgress> {
    val info = try {
      updateManager.requestAppUpdateInfo()
    } catch (e: Exception) {
      bark(LogPriority.WARN, throwable = e) { "Unable to start the Play in-app update flow" }
      return flowOf(AppUpdateProgress(-1L, -1L, Status.Failed))
    }

    return callbackFlow {
      send(AppUpdateProgress(-1L, -1L, Status.Pending))

      val listener = InstallStateUpdatedListener { state ->
        val status = when (state.installStatus()) {
          InstallStatus.PENDING -> Status.Pending
          InstallStatus.DOWNLOADING -> Status.Downloading
          InstallStatus.DOWNLOADED,
          InstallStatus.INSTALLING,
          InstallStatus.INSTALLED,
          -> Status.Downloaded
          InstallStatus.FAILED -> Status.Failed
          InstallStatus.CANCELED -> Status.Canceled
          else -> return@InstallStateUpdatedListener
        }
        trySendBlocking(
          AppUpdateProgress(
            bytes = state.bytesDownloaded(),
            totalBytes = state.totalBytesToDownload(),
            status = status,
          ),
        )
        when (state.installStatus()) {
          // Hand the downloaded update to the platform installer; this restarts the app.
          InstallStatus.DOWNLOADED -> updateManager.completeUpdate()
          InstallStatus.INSTALLED,
          InstallStatus.FAILED,
          InstallStatus.CANCELED,
          -> close()
          else -> Unit
        }
      }
      updateManager.registerListener(listener)

      // The download listener never fires when the user declines Play's confirmation
      // dialog, so surface that decision from the activity result instead.
      launch {
        val result = updateFlowLauncher.results.first()
        if (result.resultCode != Activity.RESULT_OK) {
          val status = if (result.resultCode == Activity.RESULT_CANCELED) Status.Canceled else Status.Failed
          trySendBlocking(AppUpdateProgress(-1L, -1L, status))
          close()
        }
      }

      val launcher = updateFlowLauncher.launcher
      val started = launcher != null && updateManager.startUpdateFlowForResult(
        info,
        launcher,
        AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
      )
      if (!started) {
        trySendBlocking(AppUpdateProgress(-1L, -1L, Status.Failed))
        close()
      }

      awaitClose { updateManager.unregisterListener(listener) }
    }
  }
}

/**
 * Play only reports the available versionCode, so recover the display name from the
 * deterministic MMmmppRR tag mapping in build-logic's Versioning.kt
 * (1000003 -> "1.0.0-rc3", 1000099 -> "1.0.0").
 */
private fun Int.toVersionName(): String {
  val major = this / 1_000_000
  val minor = this / 10_000 % 100
  val patch = this / 100 % 100
  val rc = this % 100
  return if (rc == 99) "$major.$minor.$patch" else "$major.$minor.$patch-rc$rc"
}
