package app.campfire.sessions.ui.expanded.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.extensions.readoutFormat
import app.campfire.common.compose.extensions.thresholdReadoutFormat
import app.campfire.common.compose.extensions.timeAgo
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.KeyboardDoubleArrowDownSemiBold
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.core.extensions.asDateTime
import app.campfire.core.extensions.asSeconds
import app.campfire.core.extensions.readableFormat
import app.campfire.core.extensions.seconds
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.Session
import app.campfire.core.model.preview.libraryItem
import app.campfire.core.model.preview.mediaProgress
import app.campfire.sessions.ui.sheets.speed.readable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@Composable
internal fun AvailableSyncButton(
  currentTime: Duration,
  mediaProgress: MediaProgress,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val shape = MaterialTheme.shapes.medium
  Surface(
    modifier = modifier,
    onClick = onClick,
    shape = shape,
    border = BorderStroke(
      width = 2.dp,
      color = MaterialTheme.colorScheme.secondary,
    ),
    color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(16.dp),
      modifier = Modifier
        .padding(
          horizontal = 16.dp,
          vertical = 8.dp,
        ),
    ) {
      Icon(
        Icons.Rounded.CloudSync,
        contentDescription = null,
        modifier = Modifier.size(20.dp),
      )

      val diff = mediaProgress.currentTime.seconds - currentTime
      val sign = if (diff > Duration.ZERO) "+" else ""
      val diffColor = if (diff > Duration.ZERO) {
        CampfireTheme.colorScheme.success
      } else {
        MaterialTheme.colorScheme.error
      }

      Column {
        Text(
          text = "Sync Available from ${mediaProgress.lastUpdate.timeAgo}",
          style = MaterialTheme.typography.labelLarge,
          fontStyle = FontStyle.Italic,
          fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(2.dp))
        Text(
          text = buildAnnotatedString {
            append("Update to ${mediaProgress.currentTime.seconds.readoutFormat()}")
            append("   ")
            withStyle(
              SpanStyle(
                fontWeight = FontWeight.Bold,
                color = diffColor,
              ),
            ) {
              append("$sign${diff.thresholdReadoutFormat()}")
            }
          },
          style = MaterialTheme.typography.labelMedium,
          fontStyle = FontStyle.Italic,
          color = LocalContentColor.current.copy(alpha = .8f),
        )
      }
    }
  }
}

@Preview
@Composable
fun AvailableSyncButtonPreview() {
  CampfireTheme {
    Surface {
      AvailableSyncButton(
        currentTime = 15.minutes,
        mediaProgress = mediaProgress(
          progress = 0.423f,
          duration = 20.hours,
        ),
        onClick = {},
        modifier = Modifier
          .padding(16.dp),
      )
    }
  }
}
