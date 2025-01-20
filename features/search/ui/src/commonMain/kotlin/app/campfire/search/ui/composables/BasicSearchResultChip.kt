package app.campfire.search.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.campfire.core.model.BasicSearchResult

@Composable
internal fun BasicSearchResultChip(
  result: BasicSearchResult,
  modifier: Modifier = Modifier,
) {
  ChipBox(
    modifier = modifier,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Spacer(Modifier.width(8.dp))
      Text(result.name)
      Spacer(Modifier.width(8.dp))
      Text(
        text = result.numItems.toString(),
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier
          .background(
            color = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(8.dp),
          )
          .padding(
            horizontal = 6.dp,
            vertical = 4.dp,
          ),
      )
    }
  }
}

@Composable
internal fun ChipBox(
  modifier: Modifier = Modifier,
  containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
  contentColor: Color = MaterialTheme.colorScheme.contentColorFor(containerColor),
  content: @Composable BoxScope.() -> Unit,
) {
  Box(
    modifier = modifier
      .background(
        color = containerColor,
        shape = RoundedCornerShape(12.dp),
      )
      .padding(
        horizontal = 8.dp,
        vertical = 6.dp,
      ),
  ) {
    CompositionLocalProvider(
      LocalTextStyle provides MaterialTheme.typography.labelLarge,
      LocalContentColor provides contentColor,
    ) {
      content()
    }
  }
}
