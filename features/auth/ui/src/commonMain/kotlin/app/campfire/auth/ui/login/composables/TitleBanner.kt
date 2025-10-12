package app.campfire.auth.ui.login.composables

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
internal fun TitleBanner(
  modifier: Modifier = Modifier,
) = SharedElementTransitionScope {
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.aligned(Alignment.CenterHorizontally),
  ) {
    NoisyCampfireIcon(
      modifier = Modifier
        .sharedElement(
          sharedContentState = rememberSharedContentState(AuthSharedTransitionKey(Logo)),
          animatedVisibilityScope = requireAnimatedScope(Navigation),
        )
        .size(96.dp),
    )
    Spacer(Modifier.width(16.dp))
    Text(
      text = stringResource(Res.string.welcome_title),
      style = MaterialTheme.typography.displayMedium,
      fontFamily = PaytoneOneFontFamily,
      modifier = Modifier
        .sharedBounds(
          sharedContentState = rememberSharedContentState(AuthSharedTransitionKey(Title)),
          animatedVisibilityScope = requireAnimatedScope(Navigation),
        ),
    )
  }
}
