package app.campfire.common.compose.icons.rounded

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons
import org.jetbrains.compose.ui.tooling.preview.Preview

val CampfireIcons.Rounded.FatCheck: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "FatCheck",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveToRelative(421f, 517f)
      lineToRelative(-60f, -60f)
      quadToRelative(-17f, -17f, -42f, -17f)
      reflectiveQuadToRelative(-42f, 17f)
      quadToRelative(-17f, 17f, -16.5f, 42f)
      reflectiveQuadToRelative(17.5f, 42f)
      lineToRelative(98f, 98f)
      quadToRelative(19.36f, 19f, 45.18f, 19f)
      reflectiveQuadTo(466f, 639f)
      lineToRelative(211f, -211f)
      quadToRelative(17f, -17f, 17f, -41.5f)
      reflectiveQuadTo(677f, 345f)
      quadToRelative(-17f, -17f, -42f, -17f)
      reflectiveQuadToRelative(-42f, 17f)
      lineTo(421f, 517f)
      close()
    }
  }.build()
}

@Preview
@Composable
private fun CheckCirclePreview() {
  Box(modifier = Modifier.padding(12.dp)) {
    Image(imageVector = CampfireIcons.Rounded.FatCheck, contentDescription = null)
  }
}
