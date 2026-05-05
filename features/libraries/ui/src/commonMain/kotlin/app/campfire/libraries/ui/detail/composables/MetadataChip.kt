package app.campfire.libraries.ui.detail.composables

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

@Composable
internal fun MetadataChip(
  modifier: Modifier = Modifier,
  containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
  borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
  content: @Composable () -> Unit,
) {
  Surface(
    color = containerColor,
    shape = MaterialTheme.shapes.small,
    border = BorderStroke(
      width = 1.dp,
      color = borderColor,
    ),
    modifier = modifier,
  ) {
    CompositionLocalProvider(
      LocalContentColor provides MaterialTheme.colorScheme.contentColorFor(containerColor),
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
