package app.campfire.android.updates

import android.app.Application
import app.campfire.core.app.ApplicationInfo
import app.campfire.core.di.AppScope
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.updates.source.AppUpdate
import app.campfire.updates.source.AppUpdateProgress
import app.campfire.updates.source.AppUpdateProgress.Status
import app.campfire.updates.source.AppUpdateSource
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.appdistribution.FirebaseAppDistribution
import com.google.firebase.appdistribution.FirebaseAppDistributionException
import com.google.firebase.appdistribution.UpdateStatus
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import me.tatarka.inject.annotations.Inject

@ContributesBinding(AppScope::class, replaces = [NoOpUpdateSource::class])
@Inject
class FirebaseAppUpdateSource(
  private val application: Application,
  private val appInfo: ApplicationInfo,
) : AppUpdateSource {

  private val appDistribution by lazy {
    FirebaseApp.initializeApp(application)
    FirebaseAppDistribution.getInstance()
  }

  override fun isSignedIn(): Boolean {
    return appDistribution.isTesterSignedIn
  }

  override suspend fun signIn() {
    try {
      appDistribution.signInTester()
        .await()
    } catch (e: FirebaseAppDistributionException) {
      bark(LogPriority.WARN, throwable = e) { "Unable to sign-in" }
    }
  }

  override suspend fun isUpdateAvailable(): Boolean {
    try {
      val update = appDistribution.checkForNewRelease().await()
      return update.versionCode > appInfo.versionCode
    } catch (e: Exception) {
      return false
    }
  }

  override suspend fun getAvailableUpdate(): AppUpdate? {
    try {
      val release = appDistribution.checkForNewRelease().await() ?: return null
      return AppUpdate(
        versionName = release.displayVersion,
        versionCode = release.versionCode,
        releaseNotes = release.releaseNotes,
      )
    } catch (_: FirebaseException) {
      return null
    }
  }

  override suspend fun installUpdate(): Flow<AppUpdateProgress> {
    return callbackFlow {
      // Make sure our UI updates immediately
      send(AppUpdateProgress(-1L, -1L, Status.Pending))

      // Start the update process
      appDistribution.updateApp()
        .addOnProgressListener { progress ->
          trySendBlocking(
            AppUpdateProgress(
              bytes = progress.apkBytesDownloaded,
              totalBytes = progress.apkFileTotalBytes,
              status = when (progress.updateStatus) {
                UpdateStatus.PENDING -> Status.Pending
                UpdateStatus.DOWNLOADING -> Status.Downloading
                UpdateStatus.DOWNLOADED -> Status.Downloaded

                UpdateStatus.INSTALL_FAILED,
                UpdateStatus.DOWNLOAD_FAILED,
                -> Status.Failed
                UpdateStatus.INSTALL_CANCELED,
                UpdateStatus.UPDATE_CANCELED,
                -> Status.Canceled

                UpdateStatus.REDIRECTED_TO_PLAY,
                UpdateStatus.NEW_RELEASE_NOT_AVAILABLE,
                UpdateStatus.NEW_RELEASE_CHECK_FAILED,
                -> Status.Canceled
              },
            ),
          )
        }
        .await()
    }
  }
}
