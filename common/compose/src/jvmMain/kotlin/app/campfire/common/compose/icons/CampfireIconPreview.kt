package app.campfire.common.compose.icons

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.animated.AnimatedTimerPainter

@Preview
@Composable
fun CampfireIconPreview() {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Image(
      CampfireIcons.Campfire,
      contentDescription = null,
    )

    Image(
      CampfireIcons.Campfire,
      contentDescription = null,
      modifier = Modifier.size(120.dp),
    )

    Image(
      CampfireIcons.Campfire,
      contentDescription = null,
      modifier = Modifier
        .size(240.dp),
    )
  }
}

@Preview
@Composable
fun TimerIconPreview() {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Icon(
      AnimatedTimerPainter,
      contentDescription = null,
      modifier = Modifier.size(120.dp),
    )
  }
}
