package app.campfire.android

import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.browser.customtabs.CustomTabsIntent
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
import com.r0adkll.kimchi.annotations.ContributesBinding
import me.tatarka.inject.annotations.Inject

class MainActivity : ComponentActivity() {

  private lateinit var component: ActivityComponent

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
    bark { "MainActivity::onCreate()" }

    component = ComponentHolder.component<ActivityComponent.Factory>()
      .create(this)
      .also {
        ComponentHolder.updateComponent(lifecycleScope, it)
      }

    // Initialize the CastContext used for Google Cast
    // https://developers.google.com/cast/docs/android_sender/integrate#kotlin
    component.mediaRouterCastController.initialize()

    WindowCompat.setDecorFitsSystemWindows(window, false)

    val toaster = AndroidToast(this)

    setContent {
      CompositionLocalProvider(
        LocalToast provides toaster,
      ) {
        component.campfireContent(
          ::finish,
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
    with(component) {
      mediaControllerConnector.connect()
      mediaRouterCastController.scanForDevices()
    }
  }

  override fun onStop() {
    super.onStop()
    bark { "MainActivity::onStop()" }
    with(component) {
      mediaControllerConnector.disconnect()
      mediaRouterCastController.stopScanningForDevices()
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    bark { "MainActivity::onDestroy()" }
    component.mediaRouterCastController.destroy()
  }
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
