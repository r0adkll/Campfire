package app.campfire.android

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import app.campfire.android.di.ActivityComponent
import app.campfire.common.screens.WelcomeScreen
import app.campfire.core.di.ComponentHolder
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.rememberCircuitNavigator

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
      val backstack = rememberSaveableBackStack(listOf(WelcomeScreen))
      val navigator = rememberCircuitNavigator(backstack)

      component.campfireContent(
        backstack,
        navigator,
        { url: String ->
          val intent = CustomTabsIntent.Builder().build()
          intent.launchUrl(this@MainActivity, Uri.parse(url))
        },
        Modifier,
      )
    }
  }
}

