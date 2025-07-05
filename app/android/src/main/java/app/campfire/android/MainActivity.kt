package app.campfire.android

import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import app.campfire.android.di.ActivityComponent
import app.campfire.android.toast.AndroidToast
import app.campfire.common.compose.toast.LocalToast
import app.campfire.core.ActivityIntentProvider
import app.campfire.core.di.AppScope
import app.campfire.core.di.ComponentHolder
import app.campfire.core.logging.bark
import com.google.firebase.appdistribution.FirebaseAppDistribution
import com.google.firebase.appdistribution.FirebaseAppDistributionException
import com.r0adkll.kimchi.annotations.ContributesBinding
import me.tatarka.inject.annotations.Inject

class MainActivity : ComponentActivity() {

  private lateinit var component: ActivityComponent

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    bark { "MainActivity::onCreate()" }

    component = ComponentHolder.component<ActivityComponent.Factory>()
      .create(this)
      .also {
        ComponentHolder.updateComponent(lifecycleScope, it)
      }

    WindowCompat.setDecorFitsSystemWindows(window, false)

    val toaster = AndroidToast(this)

    setContent {
      CompositionLocalProvider(
        LocalToast provides toaster,
      ) {
        component.campfireContent(
          backDispatcherRootPop(),
          { url: String ->
            val intent = CustomTabsIntent.Builder().build()
            intent.launchUrl(this@MainActivity, url.toUri())
          },
          Modifier,
        )
      }
    }
  }

  override fun onStart() {
    super.onStart()
    bark { "MainActivity::onStart()" }
    component.mediaControllerConnector.connect()
  }

  override fun onResume() {
    super.onResume()

    val firebaseAppDistribution = FirebaseAppDistribution.getInstance()
    firebaseAppDistribution.updateIfNewReleaseAvailable()
      .addOnProgressListener { updateProgress ->
        // (Optional) Implement custom progress updates in addition to
        // automatic NotificationManager updates.
      }
      .addOnFailureListener { e ->
        // (Optional) Handle errors.
        if (e is FirebaseAppDistributionException) {
          when (e.errorCode) {
            FirebaseAppDistributionException.Status.NOT_IMPLEMENTED -> {
              // SDK did nothing. This is expected when building for Play.
            }
            else -> {
              bark { "Error updating to a new release: ${e.message}" }
            }
          }
        }
      }
  }

  override fun onStop() {
    super.onStop()
    bark { "MainActivity::onStop()" }
    component.mediaControllerConnector.disconnect()
  }

  override fun onDestroy() {
    super.onDestroy()
    bark { "MainActivity::onDestroy()" }
  }
}

@Composable
private fun backDispatcherRootPop(): () -> Unit {
  val onBackPressedDispatcher =
    LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
      ?: error("No OnBackPressedDispatcherOwner found, unable to handle root navigation pops.")
  return { onBackPressedDispatcher.onBackPressed() }
}

@ContributesBinding(AppScope::class)
@Inject
class MainActivityIntentProvider(
  private val application: Application,
) : ActivityIntentProvider {

  override fun provide(): Intent {
    return Intent(application, MainActivity::class.java)
  }
}
