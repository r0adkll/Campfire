package app.campfire.ui.theming.ui.builder.composables

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedToggleButton
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.r0adkll.swatchbuckler.color.dynamiccolor.ColorSpec.SpecVersion

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ColorSpecPicker(
  spec: SpecVersion,
  onSpecClick: (SpecVersion) -> Unit,
  modifier: Modifier = Modifier,
) {
  val options = listOf(
    SpecVersion.SPEC_2021,
    SpecVersion.SPEC_2025,
  )

  Row(
    modifier = modifier
      .fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    options.forEach { specVersion ->
      val interactionSource = remember(specVersion) { MutableInteractionSource() }

      OutlinedToggleButton(
        checked = specVersion == spec,
        onCheckedChange = { onSpecClick(specVersion) },
        colors = ToggleButtonDefaults.outlinedToggleButtonColors(
          checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
          checkedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        interactionSource = interactionSource,
      ) {
        if (specVersion == SpecVersion.SPEC_2025) {
          Icon(
            Icons.Rounded.AutoAwesome,
            contentDescription = null,
            modifier = Modifier.size(ToggleButtonDefaults.IconSize),
          )
          Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
        }

        Text(
          when (specVersion) {
            SpecVersion.SPEC_2021 -> "2021"
            SpecVersion.SPEC_2025 -> "2025"
          },
        )
      }
    }
  }
}
