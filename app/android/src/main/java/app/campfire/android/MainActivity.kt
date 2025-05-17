package app.campfire.android

import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import app.campfire.android.di.ActivityComponent
import app.campfire.android.di.AndroidAppComponent
import app.campfire.audioplayer.impl.SessionActivityIntentProvider
import app.campfire.core.di.AppScope
import app.campfire.core.di.ComponentHolder
import app.campfire.core.logging.bark
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.asContribution

class MainActivity : ComponentActivity() {

  private lateinit var component: ActivityComponent

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    bark { "MainActivity::onCreate()" }

    component = ComponentHolder.component<AndroidAppComponent>()
      .asContribution<ActivityComponent.Factory>()
      .create(this)
      .also {
        ComponentHolder.updateComponent(lifecycleScope, it)
      }

    WindowCompat.setDecorFitsSystemWindows(window, false)

    setContent {
      component.campfireContentProvider.Content(
        backDispatcherRootPop(),
        { url: String ->
          val intent = CustomTabsIntent.Builder().build()
          intent.launchUrl(this@MainActivity, url.toUri())
        },
        Modifier,
      )
    }
  }

  override fun onStart() {
    super.onStart()
    bark { "MainActivity::onStart()" }
    component.mediaControllerConnector.connect()
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
) : SessionActivityIntentProvider {

  override fun provide(): Intent {
    return Intent(application, MainActivity::class.java)
  }
}
