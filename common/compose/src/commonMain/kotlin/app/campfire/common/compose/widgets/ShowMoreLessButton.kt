package app.campfire.common.compose.widgets

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.UnfoldLess
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import campfire.common.compose.generated.resources.Res
import campfire.common.compose.generated.resources.action_show_less
import campfire.common.compose.generated.resources.action_show_more
import org.jetbrains.compose.resources.stringResource

@Composable
fun ShowMoreLessButton(
  expanded: Boolean,
  onExpandedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  color: Color = MaterialTheme.colorScheme.primary,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
      .border(
        width = 2.dp,
        color = color,
        shape = RoundedCornerShape(8.dp),
      )
      .clip(RoundedCornerShape(8.dp))
      .clickable {
        onExpandedChange(!expanded)
      }
      .height(40.dp)
      .padding(
        start = 8.dp,
        end = 16.dp,
      ),
  ) {
    AnimatedContent(
      targetState = expanded,
      transitionSpec = {
        fadeIn(tween(90)) togetherWith fadeOut(tween(90))
      },
    ) { isExpanded ->
      Icon(
        if (isExpanded) Icons.Rounded.UnfoldLess else Icons.Rounded.UnfoldMore,
        contentDescription = null,
        tint = color,
      )
    }

    Spacer(Modifier.width(4.dp))

    val textRes = if (expanded) Res.string.action_show_less else Res.string.action_show_more
    Text(
      text = stringResource(textRes),
      style = MaterialTheme.typography.labelLarge,
      fontWeight = FontWeight.Medium,
      color = color,
    )
  }
}
