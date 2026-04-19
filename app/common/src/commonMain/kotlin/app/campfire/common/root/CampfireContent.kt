package app.campfire.common.root

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import app.campfire.account.api.UserSessionManager
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.root.ui.LoggedInWindow
import app.campfire.common.root.ui.LoggedOutWindow
import app.campfire.core.navigation.DeepLink
import app.campfire.core.session.UserSession
import app.campfire.settings.api.CampfireSettings
import app.campfire.ui.theming.api.AppThemeRepository
import app.campfire.ui.theming.api.ThemeManager
import com.slack.circuit.retained.LocalRetainedStateRegistry
import com.slack.circuit.retained.lifecycleRetainedStateRegistry
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

typealias CampfireContentWithInsets = @Composable (
  onRootPop: () -> Unit,
  onOpenUrl: (String) -> Unit,
  windowInsets: WindowInsets,
  deepLink: DeepLink,
  modifier: Modifier,
) -> Unit

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Inject
@Composable
fun CampfireContentWithInsets(
  @Assisted onRootPop: () -> Unit,
  @Assisted onOpenUrl: (String) -> Unit,
  @Assisted windowInsets: WindowInsets,
  @Assisted deepLink: DeepLink,
  settings: CampfireSettings,
  userSessionManager: UserSessionManager,
  themeManager: ThemeManager,
  themeRepository: AppThemeRepository,
  @Assisted modifier: Modifier = Modifier,
) {
  val appUriHandler = remember(onOpenUrl) {
    object : UriHandler {
      override fun openUri(uri: String) {
        onOpenUrl(uri)
      }
    }
  }

  CompositionLocalProvider(
    LocalWindowSizeClass provides calculateWindowSizeClass(),
    LocalRetainedStateRegistry provides lifecycleRetainedStateRegistry(),
    LocalUriHandler provides appUriHandler,
  ) {
    UserComponentContent(userSessionManager) { userComponent ->
      when (userComponent.currentUserSession) {
        is UserSession.NeedsAuthentication,
        UserSession.LoggedOut,
        -> LoggedOutWindow(
          userComponent = userComponent,
          onRootPop = onRootPop,
          windowInsets = windowInsets,
          settings = settings,
        )

        is UserSession.LoggedIn -> LoggedInWindow(
          userComponent = userComponent,
          onRootPop = onRootPop,
          onOpenUrl = onOpenUrl,
          windowInsets = windowInsets,
          deepLink = deepLink,
          settings = settings,
          themeManager = themeManager,
          themeRepository = themeRepository,
        )

        UserSession.Loading -> Unit
      }
    }
  }
}

typealias CampfireContent = @Composable (
  onRootPop: () -> Unit,
  onOpenUrl: (String) -> Unit,
  deepLink: DeepLink,
  modifier: Modifier,
) -> Unit

@Inject
@Composable
fun CampfireContent(
  @Assisted onRootPop: () -> Unit,
  @Assisted onOpenUrl: (String) -> Unit,
  @Assisted deepLink: DeepLink,
  settings: CampfireSettings,
  userSessionManager: UserSessionManager,
  themeManager: ThemeManager,
  themeRepository: AppThemeRepository,
  @Assisted modifier: Modifier = Modifier,
) {
  CampfireContentWithInsets(
    onRootPop = onRootPop,
    settings = settings,
    userSessionManager = userSessionManager,
    themeManager = themeManager,
    themeRepository = themeRepository,
    onOpenUrl = onOpenUrl,
    windowInsets = WindowInsets.systemBars
      .exclude(WindowInsets.statusBars)
      .exclude(WindowInsets.navigationBars),
    deepLink = deepLink,
    modifier = modifier,
  )
}
