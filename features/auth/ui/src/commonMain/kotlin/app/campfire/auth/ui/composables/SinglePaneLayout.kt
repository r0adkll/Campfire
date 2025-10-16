package app.campfire.auth.ui.composables

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.auth.ui.shared.AuthSharedTransitionKey
import app.campfire.auth.ui.shared.AuthSharedTransitionKey.ElementType.Logo
import app.campfire.auth.ui.shared.AuthSharedTransitionKey.ElementType.Title
import app.campfire.common.compose.icons.NoisyCampfireIcon
import app.campfire.common.compose.theme.PaytoneOneFontFamily
import campfire.features.auth.ui.generated.resources.Res
import campfire.features.auth.ui.generated.resources.welcome_title
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import com.slack.circuit.sharedelements.SharedElementTransitionScope.AnimatedScope.Navigation
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun SinglePaneLayout(
  modifier: Modifier = Modifier,
  logoTitle: @Composable ColumnScope.() -> Unit = {
    VerticalLogoTitle(
      Modifier.weight(1f),
    )
  },
  content: @Composable ColumnScope.() -> Unit,
) = SharedElementTransitionScope {
  Column(modifier.fillMaxSize()) {
    logoTitle()
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      content()
    }
  }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun VerticalLogoTitle(
  modifier: Modifier = Modifier,
) = SharedElementTransitionScope {
  Column(
    modifier = modifier.fillMaxWidth(),
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
}
