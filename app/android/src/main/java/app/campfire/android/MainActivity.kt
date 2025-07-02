package app.campfire.android

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import app.campfire.android.di.ActivityComponent
import app.campfire.core.ActivityIntentProvider
import app.campfire.core.di.AppScope
import app.campfire.core.di.ComponentHolder
import app.campfire.core.logging.bark
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

    setContent {
      component.campfireContent(
        backDispatcherRootPop(),
        { url: String ->
          val intent = CustomTabsIntent.Builder().build()
          intent.launchUrl(this@MainActivity, Uri.parse(url))
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
) : ActivityIntentProvider {

  override fun provide(): Intent {
    return Intent(application, MainActivity::class.java)
  }
}
