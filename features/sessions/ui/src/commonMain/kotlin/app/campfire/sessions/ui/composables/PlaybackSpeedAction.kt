package app.campfire.sessions.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.campfire.sessions.ui.sheets.speed.readable

@Composable
fun PlaybackSpeedAction(
  playbackSpeed: Float,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  if (playbackSpeed == 1f) {
    IconButton(
      onClick = onClick,
      modifier = modifier,
    ) {
      Icon(Icons.Rounded.Speed, contentDescription = null)
    }
  } else {
    Box(
      modifier = modifier
        .clip(RoundedCornerShape(16.dp))
        .width(96.dp)
        .height(48.dp)
        .clickable(onClick = onClick),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = "${playbackSpeed.readable}x",
        fontSize = 18.sp,
        fontWeight = FontWeight.ExtraBold,
      )
    }
  }
}
