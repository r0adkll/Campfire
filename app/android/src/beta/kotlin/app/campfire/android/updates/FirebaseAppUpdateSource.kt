package app.campfire.android.updates

import app.campfire.core.app.ApplicationInfo
import app.campfire.core.di.AppScope
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.updates.source.AppUpdateSource
import com.google.firebase.appdistribution.FirebaseAppDistribution
import com.google.firebase.appdistribution.FirebaseAppDistributionException
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.tasks.await
import me.tatarka.inject.annotations.Inject

@ContributesBinding(AppScope::class)
@Inject
class FirebaseAppUpdateSource(
  private val appInfo: ApplicationInfo,
) : AppUpdateSource {

  private val appDistribution by lazy {
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

  override suspend fun installUpdate() {
    appDistribution.updateApp().await()
  }
}
