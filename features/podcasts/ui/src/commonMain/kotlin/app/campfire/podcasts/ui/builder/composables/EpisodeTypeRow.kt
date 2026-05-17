package app.campfire.podcasts.ui.builder.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.podcasts.ui.builder.EpisodeType
import campfire.features.podcasts.ui.generated.resources.Res
import campfire.features.podcasts.ui.generated.resources.add_podcast_builder_episode_type_episodic
import campfire.features.podcasts.ui.generated.resources.add_podcast_builder_episode_type_serial
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun EpisodeTypeRow(
  selected: EpisodeType,
  onSelect: (EpisodeType) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier,
  ) {
    val options = EpisodeType.entries.toList()

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
      val modifiers = listOf(
        Modifier.weight(1f),
        Modifier.weight(1f),
      )

      options.forEachIndexed { index, label ->
        val option = options[index]
        ToggleButton(
          checked = option == selected,
          onCheckedChange = { onSelect(option) },
          modifier = modifiers[index]
            .semantics { role = Role.RadioButton },
          shapes =
          when (index) {
            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
            options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
          },
          colors = ToggleButtonDefaults.tonalToggleButtonColors(),
        ) {
          Text(stringResource(label.labelResource()))
        }
      }
    }
  }
}

internal fun EpisodeType.labelResource(): StringResource = when (this) {
  EpisodeType.Episodic -> Res.string.add_podcast_builder_episode_type_episodic
  EpisodeType.Serial -> Res.string.add_podcast_builder_episode_type_serial
}

@Preview
@Composable
fun PodcastTypeRow() {
  CampfireTheme {
    EpisodeTypeRow(
      selected = EpisodeType.Episodic,
      onSelect = {},
    )
  }
}
