package app.campfire.common.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.rounded.rememberAnimatedEmptyState
import app.campfire.common.compose.theme.CampfireTheme

val color1 = Color(0xFFA7B3C7)
val color2 = Color(0xFF68E5FD)
val color3 = Color(0xFF98C900)
val color4 = Color(0xFFDA7200)
val color5 = Color(0xFF5E8700)
val color6 = Color(0xFFFFCE29)
val color7 = Color(0xFFFFA500)

@Preview
@Composable
fun EmptyStatePainterPreview() {
  CampfireTheme(
    useDarkColors = false,
  ) {
    Surface {
      Image(
        painter = rememberAnimatedEmptyState(),
        contentDescription = null,
        modifier = Modifier.size(200.dp),
      )
    }
  }
}
