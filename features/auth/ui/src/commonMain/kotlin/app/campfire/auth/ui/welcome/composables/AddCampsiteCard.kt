package app.campfire.auth.ui.welcome.composables

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.auth.ui.shared.AuthSharedTransitionKey
import app.campfire.auth.ui.shared.AuthSharedTransitionKey.ElementType.Tent
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.Red
import campfire.features.auth.ui.generated.resources.Res
import campfire.features.auth.ui.generated.resources.welcome_add_campsite_subtitle
import campfire.features.auth.ui.generated.resources.welcome_add_campsite_title
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import com.slack.circuit.sharedelements.SharedElementTransitionScope.AnimatedScope.Navigation
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun AddCampsiteCard(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) = SharedElementTransitionScope {
  ElevatedCard(
    modifier = modifier,
    onClick = onClick,
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ),
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Image(
        CampfireIcons.Tents.Red,
        contentDescription = null,
        modifier = Modifier
          .sharedElement(
            sharedContentState = rememberSharedContentState(AuthSharedTransitionKey(Tent)),
            animatedVisibilityScope = requireAnimatedScope(Navigation),
          ),
      )
      Spacer(Modifier.width(16.dp))
      Column(
        modifier = Modifier.weight(1f),
      ) {
        Text(
          text = stringResource(Res.string.welcome_add_campsite_title),
          style = MaterialTheme.typography.titleMedium,
        )
        Text(
          text = stringResource(Res.string.welcome_add_campsite_subtitle),
          style = MaterialTheme.typography.bodyMedium,
        )
      }
    }
  }
}
