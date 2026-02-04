package app.campfire.sessions.ui.expanded.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.rounded.Bookmarks

@Composable
internal fun ActionRow(
  onBookmarksClick: () -> Unit,
  speedContent: @Composable () -> Unit,
  timerContent: @Composable () -> Unit,
  onChapterListClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .height(72.dp)
      .padding(horizontal = 16.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceEvenly,
  ) {
    Box(
      modifier = Modifier.weight(1f),
      contentAlignment = Alignment.Center,
    ) {
      IconButton(
        onClick = onBookmarksClick,
      ) {
        Icon(Icons.Rounded.Bookmarks, contentDescription = null)
      }
    }

    Box(
      modifier = Modifier.weight(1f),
      contentAlignment = Alignment.Center,
    ) {
      speedContent()
    }

    Box(
      modifier = Modifier.weight(1f),
      contentAlignment = Alignment.Center,
    ) {
      timerContent()
    }

    Box(
      modifier = Modifier.weight(1f),
      contentAlignment = Alignment.Center,
    ) {
      IconButton(
        onClick = onChapterListClick,
      ) {
        Icon(Icons.AutoMirrored.Rounded.List, contentDescription = null)
      }
    }
  }
}
