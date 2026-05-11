package app.campfire.common.compose.widgets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.DisabledAlpha

@Composable
fun MetadataChip(
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  colors: MetadataChipColors = MetadataChipDefaults.colors(),
  content: @Composable () -> Unit,
) {
  Surface(
    color = if (enabled) colors.containerColor else colors.disabledContainerColor,
    shape = MaterialTheme.shapes.small,
    border = BorderStroke(
      width = 1.dp,
      color = if (enabled) colors.borderColor else colors.disabledContainerColor,
    ),
    modifier = modifier,
  ) {
    CompositionLocalProvider(
      LocalContentColor provides if (enabled) colors.contentColor else colors.disabledContentColor,
      LocalTextStyle provides MaterialTheme.typography.labelSmallEmphasized,
    ) {
      Box(
        Modifier.padding(
          horizontal = 12.dp,
          vertical = 6.dp,
        ),
      ) {
        content()
      }
    }
  }
}

@ConsistentCopyVisibility
data class MetadataChipColors internal constructor(
  val containerColor: Color,
  val contentColor: Color,
  val borderColor: Color,

  val disabledContainerColor: Color,
  val disabledContentColor: Color,
  val disabledBorderColor: Color,
)

object MetadataChipDefaults {

  @Composable
  fun colors(
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor: Color = contentColorFor(containerColor),
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    disabledContainerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = DisabledAlpha),
    disabledContentColor: Color = contentColorFor(MaterialTheme.colorScheme.surfaceContainerHigh)
      .copy(alpha = DisabledAlpha),
    disabledBorderColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = DisabledAlpha),
  ): MetadataChipColors = MetadataChipColors(
    containerColor = containerColor,
    contentColor = contentColor,
    borderColor = borderColor,
    disabledContainerColor = disabledContainerColor,
    disabledContentColor = disabledContentColor,
    disabledBorderColor = disabledBorderColor,
  )
}
