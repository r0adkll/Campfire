package app.campfire.common.compose.icons

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.campfire.common.compose.icons.rounded.CastConnecting
import app.campfire.common.compose.theme.CampfireTheme

@Preview
@Composable
fun CastConnectingIconPreview() {
  CampfireTheme {
    Surface {
      Column {
        Icon(
          CampfireIcons.Rounded.CastConnecting,
          contentDescription = null,
        )
      }
    }
  }
}
