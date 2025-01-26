package app.campfire.auth.ui.welcome.composables

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
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.Red
import campfire.features.auth.ui.generated.resources.Res
import campfire.features.auth.ui.generated.resources.welcome_add_campsite_subtitle
import campfire.features.auth.ui.generated.resources.welcome_add_campsite_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AddCampsiteCard(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
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
