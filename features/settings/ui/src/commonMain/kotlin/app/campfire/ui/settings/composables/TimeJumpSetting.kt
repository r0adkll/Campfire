package app.campfire.ui.settings.composables

import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import kotlin.time.Duration

@Composable
internal fun TimeJumpSetting(
  time: Duration,
  onTimeChange: (Duration) -> Unit,
  jumps: TimeJumps,
  headlineContent: @Composable () -> Unit,
  supportingContent: (@Composable () -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  SettingListItem(
    headlineContent = headlineContent,
    supportingContent = supportingContent,
    trailingContent = {
      Text(
        text = time.toString(),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
      )
    },
    modifier = modifier
      .clickable {
        val nextJump = jumps.nextFrom(time)
        onTimeChange(nextJump)
      },
  )
}

/**
 * An interface to provide custom time jumps for this setting
 */
internal interface TimeJumps {
  fun nextFrom(duration: Duration): Duration
}
