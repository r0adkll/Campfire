package app.campfire.common.compose.icons.rounded

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.Group
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons
import org.jetbrains.compose.ui.tooling.preview.Preview

val CampfireIcons.Rounded.MovingDelete: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "Rounded.MovingDelete",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveTo(10f, 2f)
      lineTo(9f, 3f)
      lineTo(5f, 3f)
      curveTo(4.448f, 3f, 4f, 3.448f, 4f, 4f)
      curveTo(4f, 4.552f, 4.448f, 5f, 5f, 5f)
      lineTo(19f, 5f)
      curveTo(19.552f, 5f, 20f, 4.552f, 20f, 4f)
      curveTo(20f, 3.448f, 19.552f, 3f, 19f, 3f)
      lineTo(15f, 3f)
      lineTo(14f, 2f)
      lineTo(10f, 2f)
      close()
      moveTo(5f, 7f)
      lineTo(5f, 20f)
      curveTo(5f, 21.1f, 5.9f, 22f, 7f, 22f)
      lineTo(17f, 22f)
      curveTo(18.1f, 22f, 19f, 21.1f, 19f, 20f)
      lineTo(19f, 7f)
      lineTo(5f, 7f)
      close()
      moveTo(9.41f, 10.414f)
      curveTo(9.665f, 10.414f, 9.921f, 10.511f, 10.115f, 10.705f)
      lineTo(12f, 12.59f)
      lineTo(13.885f, 10.705f)
      curveTo(14.274f, 10.316f, 14.906f, 10.316f, 15.295f, 10.705f)
      curveTo(15.684f, 11.094f, 15.684f, 11.726f, 15.295f, 12.115f)
      lineTo(13.41f, 14f)
      lineTo(15.295f, 15.885f)
      curveTo(15.684f, 16.274f, 15.684f, 16.906f, 15.295f, 17.295f)
      curveTo(14.906f, 17.684f, 14.274f, 17.684f, 13.885f, 17.295f)
      lineTo(12f, 15.41f)
      lineTo(10.115f, 17.295f)
      curveTo(9.726f, 17.684f, 9.094f, 17.684f, 8.705f, 17.295f)
      curveTo(8.316f, 16.906f, 8.316f, 16.274f, 8.705f, 15.885f)
      lineTo(10.59f, 14f)
      lineTo(8.705f, 12.115f)
      curveTo(8.316f, 11.726f, 8.316f, 11.094f, 8.705f, 10.705f)
      curveTo(8.9f, 10.511f, 9.155f, 10.414f, 9.41f, 10.414f)
      close()
    }
  }.build()
}

@Composable
fun rememberMovingDeletePainter(
  rotationInDegrees: Float = 0f,
) = rememberVectorPainter(
  defaultWidth = 56.dp,
  defaultHeight = 56.dp,
  viewportWidth = 56f,
  viewportHeight = 56f,
  autoMirror = true,
  name = "MovingDelete",
) { _, _ ->
  val cappedRotation = rotationInDegrees.coerceIn(0f, 30f)

  Group(
    name = "Lid",
    rotation = -cappedRotation,
    pivotX = 0f,
    pivotY = 10f,
    translationX = 16f,
    translationY = 16f,
  ) {
    Path(fill = SolidColor(Color.Black)) {
      moveTo(10f, 2f)
      lineTo(9f, 3f)
      lineTo(5f, 3f)
      curveTo(4.448f, 3f, 4f, 3.448f, 4f, 4f)
      curveTo(4f, 4.552f, 4.448f, 5f, 5f, 5f)
      lineTo(19f, 5f)
      curveTo(19.552f, 5f, 20f, 4.552f, 20f, 4f)
      curveTo(20f, 3.448f, 19.552f, 3f, 19f, 3f)
      lineTo(15f, 3f)
      lineTo(14f, 2f)
      lineTo(10f, 2f)
      close()
    }
  }
  Group(
    name = "Bin",
    translationX = 16f,
    translationY = 16f,
  ) {
    Path(fill = SolidColor(Color.Black)) {
      moveTo(5f, 7f)
      lineTo(5f, 20f)
      curveTo(5f, 21.1f, 5.9f, 22f, 7f, 22f)
      lineTo(17f, 22f)
      curveTo(18.1f, 22f, 19f, 21.1f, 19f, 20f)
      lineTo(19f, 7f)
      lineTo(5f, 7f)
      close()
      moveTo(9.41f, 10.414f)
      curveTo(9.665f, 10.414f, 9.921f, 10.511f, 10.115f, 10.705f)
      lineTo(12f, 12.59f)
      lineTo(13.885f, 10.705f)
      curveTo(14.274f, 10.316f, 14.906f, 10.316f, 15.295f, 10.705f)
      curveTo(15.684f, 11.094f, 15.684f, 11.726f, 15.295f, 12.115f)
      lineTo(13.41f, 14f)
      lineTo(15.295f, 15.885f)
      curveTo(15.684f, 16.274f, 15.684f, 16.906f, 15.295f, 17.295f)
      curveTo(14.906f, 17.684f, 14.274f, 17.684f, 13.885f, 17.295f)
      lineTo(12f, 15.41f)
      lineTo(10.115f, 17.295f)
      curveTo(9.726f, 17.684f, 9.094f, 17.684f, 8.705f, 17.295f)
      curveTo(8.316f, 16.906f, 8.316f, 16.274f, 8.705f, 15.885f)
      lineTo(10.59f, 14f)
      lineTo(8.705f, 12.115f)
      curveTo(8.316f, 11.726f, 8.316f, 11.094f, 8.705f, 10.705f)
      curveTo(8.9f, 10.511f, 9.155f, 10.414f, 9.41f, 10.414f)
      close()
    }
  }
}

@Preview
@Composable
fun MovingDeletePreview() {
  Box(modifier = Modifier.padding(12.dp)) {
    Image(
      rememberMovingDeletePainter(),
      contentDescription = null,
      modifier = Modifier
        .size(56.dp)
        .padding(16.dp),
    )
  }
}
