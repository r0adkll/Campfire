package app.campfire.auth.ui.welcome

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.auth.ui.composables.MaxContentWidth
import app.campfire.auth.ui.composables.SinglePaneLayout
import app.campfire.auth.ui.composables.TwoPaneLayout
import app.campfire.auth.ui.login.LoginUiContent
import app.campfire.auth.ui.shared.AuthSharedTransitionKey
import app.campfire.auth.ui.shared.AuthSharedTransitionKey.ElementType.Card
import app.campfire.auth.ui.welcome.composables.AddCampsiteCard
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.common.screens.WelcomeScreen
import app.campfire.core.di.UserScope
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import com.slack.circuit.sharedelements.SharedElementTransitionScope.AnimatedScope.Navigation

@OptIn(ExperimentalSharedTransitionApi::class)
@CircuitInject(WelcomeScreen::class, UserScope::class)
@Composable
fun Welcome(
  state: WelcomeUiState,
  modifier: Modifier,
) = SharedElementTransitionScope {
  val windowSizeClass = LocalWindowSizeClass.current

  CampfireTheme(
    tent = state.loginUiState.tent,
  ) {
    if (windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Large) {
      TwoPaneLayout(modifier) {
        LoginUiContent(
          state = state.loginUiState,
          modifier = Modifier.align(Alignment.CenterStart),
        )
      }
    } else {
      SinglePaneLayout(
        modifier = modifier,
      ) {
        AddCampsiteCard(
          onClick = { state.eventSink(WelcomeUiEvent.AddCampsite) },
          modifier = Modifier
            .sharedBounds(
              sharedContentState = rememberSharedContentState(AuthSharedTransitionKey(Card)),
              animatedVisibilityScope = requireAnimatedScope(Navigation),
            )
            .widthIn(max = MaxContentWidth)
            .fillMaxWidth()
            .padding(
              horizontal = 26.dp,
            ),
        )
      }
    }
  }
}
