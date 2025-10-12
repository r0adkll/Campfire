package app.campfire.auth.ui.welcome

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.auth.ui.login.LoginUiContent
import app.campfire.auth.ui.login.LoginUiState
import app.campfire.auth.ui.shared.AuthSharedTransitionKey
import app.campfire.auth.ui.shared.AuthSharedTransitionKey.ElementType.Card
import app.campfire.auth.ui.shared.AuthSharedTransitionKey.ElementType.Logo
import app.campfire.auth.ui.shared.AuthSharedTransitionKey.ElementType.Title
import app.campfire.auth.ui.welcome.composables.AddCampsiteCard
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.icons.NoisyCampfireIcon
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.common.compose.theme.PaytoneOneFontFamily
import app.campfire.common.screens.WelcomeScreen
import app.campfire.core.di.UserScope
import campfire.features.auth.ui.generated.resources.Res
import campfire.features.auth.ui.generated.resources.welcome_title
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import com.slack.circuit.sharedelements.SharedElementTransitionScope.AnimatedScope.Navigation
import org.jetbrains.compose.resources.stringResource

@CircuitInject(WelcomeScreen::class, UserScope::class)
@Composable
fun Welcome(
  state: WelcomeUiState,
  modifier: Modifier,
) {
  val windowSizeClass = LocalWindowSizeClass.current
  val eventSink = state.eventSink

  CampfireTheme(
    tent = state.loginUiState.tent,
  ) {
    if (windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Large) {
      TwoPaneLayout(
        loginUiState = state.loginUiState,
        modifier = modifier,
      )
    } else {
      SinglePaneLayout(
        onEvent = eventSink,
        modifier = modifier,
      )
    }
  }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SinglePaneLayout(
  onEvent: (WelcomeUiEvent) -> Unit,
  modifier: Modifier = Modifier,
) = SharedElementTransitionScope {
  Column(modifier.fillMaxSize()) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Bottom,
    ) {
      NoisyCampfireIcon(
        modifier = Modifier
          .sharedElement(
            sharedContentState = rememberSharedContentState(AuthSharedTransitionKey(Logo)),
            animatedVisibilityScope = requireAnimatedScope(Navigation),
          )
          .size(236.dp),
      )

      Spacer(Modifier.height(16.dp))

      Text(
        text = stringResource(Res.string.welcome_title),
        style = MaterialTheme.typography.displayLarge,
        fontFamily = PaytoneOneFontFamily,
        modifier = Modifier
          .sharedBounds(
            sharedContentState = rememberSharedContentState(AuthSharedTransitionKey(Title)),
            animatedVisibilityScope = requireAnimatedScope(Navigation),
          ),
      )
    }
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      AddCampsiteCard(
        onClick = { onEvent(WelcomeUiEvent.AddCampsite) },
        modifier = Modifier
          .sharedBounds(
            sharedContentState = rememberSharedContentState(AuthSharedTransitionKey(Card)),
            animatedVisibilityScope = requireAnimatedScope(Navigation),
          )
          .widthIn(max = 500.dp)
          .fillMaxWidth()
          .padding(
            horizontal = 26.dp,
          ),
      )
    }
  }
}

@Composable
private fun TwoPaneLayout(
  loginUiState: LoginUiState,
  modifier: Modifier = Modifier,
) {
  Surface {
    Row(
      modifier = modifier
        .fillMaxSize(),
    ) {
      Column(
        modifier = Modifier
          .fillMaxHeight()
          .weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        NoisyCampfireIcon(
          modifier = Modifier.size(236.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
          text = stringResource(Res.string.welcome_title),
          style = MaterialTheme.typography.displayLarge,
          fontFamily = PaytoneOneFontFamily,
        )
      }

      Box(
        modifier = Modifier
          .fillMaxHeight()
          .weight(1f),
      ) {
        LoginUiContent(
          state = loginUiState,
          modifier = Modifier.align(Alignment.CenterStart),
        )
      }
    }
  }
}
