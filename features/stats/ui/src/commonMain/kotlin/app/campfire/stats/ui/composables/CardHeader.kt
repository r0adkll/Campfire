package app.campfire.stats.ui.composables

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun CardHeader(
  icon: @Composable () -> Unit,
  title: @Composable () -> Unit,
  trailing: (@Composable () -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .height(48.dp)
      .padding(horizontal = 16.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    CompositionLocalProvider(
      LocalContentColor provides MaterialTheme.colorScheme.primary,
    ) {
      icon()
    }

    Spacer(Modifier.width(16.dp))

    ProvideTextStyle(MaterialTheme.typography.titleMedium) {
      title()
    }

    if (trailing != null) {
      Spacer(Modifier.weight(1f))
      CompositionLocalProvider(
        LocalContentColor provides MaterialTheme.colorScheme.primary,
      ) {
        ProvideTextStyle(MaterialTheme.typography.titleMedium) {
          trailing()
        }
      }
    }
  }
}
