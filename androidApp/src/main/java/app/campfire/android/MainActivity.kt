package app.campfire.android

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import app.campfire.android.di.ActivityComponent
import app.campfire.core.di.ComponentHolder

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val component = ComponentHolder.component<ActivityComponent.Factory>()
      .create(this)
      .also {
        ComponentHolder.components += it
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
}

@Composable
private fun backDispatcherRootPop(): () -> Unit {
  val onBackPressedDispatcher =
    LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
      ?: error("No OnBackPressedDispatcherOwner found, unable to handle root navigation pops.")
  return { onBackPressedDispatcher.onBackPressed() }
}
