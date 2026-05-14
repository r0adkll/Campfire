package app.campfire.common.compose.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun MetadataHeader(
  title: String,
  leadingContent: (@Composable () -> Unit)? = null,
  trailingContent: (@Composable () -> Unit)? = null,
  modifier: Modifier = Modifier,
  textStyle: TextStyle = MaterialTheme.typography.titleLarge,
  fontWeight: FontWeight = FontWeight.SemiBold,
  textColor: Color = MaterialTheme.colorScheme.onSurface,
) {
  MetadataHeader(
    title = {
      Text(
        text = title,
        style = textStyle,
        fontWeight = fontWeight,
        color = textColor,
      )
    },
    leadingContent = leadingContent,
    trailingContent = trailingContent,
    modifier = modifier,
  )
}

@Composable
fun MetadataHeader(
  title: @Composable () -> Unit,
  leadingContent: (@Composable () -> Unit)? = null,
  trailingContent: (@Composable () -> Unit)? = null,
  modifier: Modifier = Modifier,
  contentColor: Color = MaterialTheme.colorScheme.onSurface,
) {
  Row(
    modifier = modifier.heightIn(min = 56.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    leadingContent?.let { leading ->
      leading()
      Spacer(Modifier.width(8.dp))
    }

    Box(Modifier.weight(1f)) {
      CompositionLocalProvider(
        LocalContentColor provides contentColor,
      ) {
        ProvideTextStyle(
          MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.SemiBold,
          ),
        ) {
          title()
        }
      }
    }

    trailingContent?.let { trailing ->
      Spacer(Modifier.width(16.dp))
      trailing()
    }
  }
}
