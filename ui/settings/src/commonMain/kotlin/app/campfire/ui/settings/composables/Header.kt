package app.campfire.ui.settings.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

internal val HorizontalSettingPadding = 16.dp
internal val VerticalSettingPadding = 16.dp

@Composable
internal fun Header(
  title: @Composable () -> Unit,
  modifier: Modifier = Modifier,
  style: TextStyle = MaterialTheme.typography.labelLarge.copy(
    fontWeight = FontWeight.SemiBold,
  ),
  color: Color = MaterialTheme.colorScheme.primary,
) {
  Box(
    modifier = modifier
      .padding(
        horizontal = HorizontalSettingPadding,
        vertical = VerticalSettingPadding,
      ),
  ) {
    val mergedStyle = LocalTextStyle.current.merge(style)
    CompositionLocalProvider(
      LocalTextStyle provides mergedStyle,
      LocalContentColor provides color,
    ) {
      title()
    }
  }
}
