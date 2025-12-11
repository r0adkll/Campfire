package app.campfire.ui.theming.ui.builder.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.r0adkll.swatchbuckler.color.dynamiccolor.ColorSpec.SpecVersion
import com.r0adkll.swatchbuckler.color.dynamiccolor.Variant

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ColorStylePicker(
  spec: SpecVersion,
  style: Variant,
  onStyleClick: (Variant) -> Unit,
  modifier: Modifier = Modifier,
) {
  val styles = mapOf(
    SpecVersion.SPEC_2025 to listOf(
      Variant.EXPRESSIVE,
      Variant.VIBRANT,
      Variant.TONAL_SPOT,
      Variant.NEUTRAL,
    ),
    SpecVersion.SPEC_2021 to listOf(
      Variant.RAINBOW,
      Variant.FRUIT_SALAD,
      Variant.MONOCHROME,
      Variant.FIDELITY,
      Variant.CONTENT,
    ),
  )

  FlowRow(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    styles.entries.forEach { (version, variants) ->
      val enabled = version != SpecVersion.SPEC_2021 || spec != SpecVersion.SPEC_2025
      variants.forEach { variant ->
        OutlinedToggleButton(
          enabled = enabled,
          checked = variant == style,
          onCheckedChange = { onStyleClick(variant) },
          colors = ToggleButtonDefaults.outlinedToggleButtonColors(
            checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            checkedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
          ),
        ) {
          if (version == SpecVersion.SPEC_2025) {
            Icon(
              Icons.Rounded.AutoAwesome,
              contentDescription = null,
              modifier = Modifier.size(ToggleButtonDefaults.IconSize),
            )
            Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
          }
          Text(
            text = when (variant) {
              Variant.MONOCHROME -> "Monochrome"
              Variant.NEUTRAL -> "Neutral"
              Variant.TONAL_SPOT -> "Tonal Spot"
              Variant.VIBRANT -> "Vibrant"
              Variant.EXPRESSIVE -> "Expressive"
              Variant.FIDELITY -> "Fidelity"
              Variant.CONTENT -> "Content"
              Variant.RAINBOW -> "Rainbow"
              Variant.FRUIT_SALAD -> "Fruit Salad"
            },
          )
        }
      }
    }
  }
}
