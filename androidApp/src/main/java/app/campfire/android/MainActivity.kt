package app.campfire.android

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import app.campfire.android.di.ActivityComponent
import app.campfire.android.di.AndroidAppComponent
import app.campfire.android.di.from
import app.campfire.common.screens.LoginScreen
import app.campfire.core.di.ActivityScope
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.shared.di.UiComponent
import app.campfire.shared.root.CampfireContent
import com.r0adkll.kimchi.annotations.ContributesSubcomponent
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.rememberCircuitNavigator
import me.tatarka.inject.annotations.Provides

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val component = AndroidAppComponent.from(this)
      .createMainActivityComponent(this)

    WindowCompat.setDecorFitsSystemWindows(window, false)

    setContent {
      val backstack = rememberSaveableBackStack(listOf(LoginScreen))
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

@SingleIn(ActivityScope::class)
@ContributesSubcomponent(
  scope = ActivityScope::class,
  parentScope = AppScope::class,
)
abstract class MainActivityComponent(
  @get:Provides override val activity: Activity,
) : ActivityComponent, UiComponent {
  abstract val campfireContent: CampfireContent
}
