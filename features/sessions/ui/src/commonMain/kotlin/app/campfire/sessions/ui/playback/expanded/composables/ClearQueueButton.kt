package app.campfire.sessions.ui.playback.expanded.composables

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Check
import app.campfire.common.compose.icons.rounded.Close
import app.campfire.common.compose.icons.rounded.DeleteSweep
import app.campfire.common.compose.theme.CampfireTheme
import campfire.features.sessions.ui.generated.resources.Res
import campfire.features.sessions.ui.generated.resources.action_clear_queue
import org.jetbrains.compose.resources.stringResource

@Composable
fun ClearQueueButton(
  onConfirmClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var showConfirmation by remember { mutableStateOf(false) }

  val buttonColor by animateColorAsState(
    if (showConfirmation) MaterialTheme.colorScheme.errorContainer else Color.Transparent,
  )

  Box(
    modifier = modifier
      .background(
        color = buttonColor,
        shape = CircleShape,
      ),
  ) {
    AnimatedContent(
      targetState = showConfirmation,
      transitionSpec = {
        (
          fadeIn(animationSpec = tween(220, delayMillis = 90)) +
            expandHorizontally(
              animationSpec = tween(220, delayMillis = 90),
              expandFrom = Alignment.Start,
            ) { 0 }
          ).togetherWith(fadeOut(animationSpec = tween(90)))
      },
    ) { confirm ->
      if (confirm) {
        ConfirmClearQueueContent(
          onConfirmClick = {
            showConfirmation = false
            onConfirmClick()
          },
          onCancelClick = { showConfirmation = false },
        )
      } else {
        IconButton(
          onClick = { showConfirmation = true },
        ) {
          Icon(
            CampfireIcons.Rounded.DeleteSweep,
            contentDescription = "Clear queue",
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AnimatedContentScope.ConfirmClearQueueContent(
  onConfirmClick: () -> Unit,
  onCancelClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    // Cancel Button
    IconButton(
      onClick = onCancelClick,
    ) {
      Icon(
        CampfireIcons.Rounded.Close,
        contentDescription = "Cancel queue clear",
        tint = MaterialTheme.colorScheme.onErrorContainer,
      )
    }

    // Text
    Text(
      text = stringResource(Res.string.action_clear_queue),
      style = MaterialTheme.typography.bodyLarge,
      fontWeight = FontWeight.Medium,
      color = MaterialTheme.colorScheme.onErrorContainer,
    )

    // Confirm Button
    FilledIconButton(
      onClick = onConfirmClick,
      colors = IconButtonDefaults.filledIconButtonColors(
        containerColor = CampfireTheme.colorScheme.success,
        contentColor = CampfireTheme.colorScheme.onSuccess,
      ),
      modifier = Modifier
        .animateEnterExit(
          enter = expandIn(
            animationSpec = tween(150, delayMillis = 180),
            expandFrom = Alignment.Center,
          ) { IntSize(0, 0) },
          exit = shrinkOut() + fadeOut(),
        ),
    ) {
      Icon(
        CampfireIcons.Rounded.Check,
        contentDescription = "Clear queue",
      )
    }
  }
}
