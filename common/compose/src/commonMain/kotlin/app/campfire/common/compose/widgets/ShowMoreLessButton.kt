@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.UnfoldLess
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
  val shape = MaterialTheme.shapes.small
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
      .border(
        width = 1.dp,
        color = color,
        shape = shape,
      )
      .clip(shape)
      .clickable {
        onExpandedChange(!expanded)
      }
      .height(ButtonDefaults.ExtraSmallContainerHeight)
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
        modifier = Modifier.size(ButtonDefaults.ExtraSmallIconSize),
      )
    }

    Spacer(Modifier.size(ButtonDefaults.ExtraSmallIconSpacing))

    val textRes = if (expanded) Res.string.action_show_less else Res.string.action_show_more
    Text(
      text = stringResource(textRes),
      style = ButtonDefaults.textStyleFor(ButtonDefaults.ExtraSmallContainerHeight),
      fontWeight = FontWeight.Medium,
      color = color,
    )
  }
}
