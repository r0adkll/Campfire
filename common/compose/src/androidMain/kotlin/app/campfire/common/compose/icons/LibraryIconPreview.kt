package app.campfire.common.compose.icons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.core.model.Library

@Preview
@Composable
fun LibraryIconsPreview() {
  CampfireTheme {
    Surface {
      Column {
        Library.Icon.entries.forEach { icon ->

          Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
          ) {
            Icon(
              icon.asComposeIcon(),
              contentDescription = null,
            )
            Text(icon.networkKey, modifier = Modifier.fillMaxWidth())
          }
        }
      }
    }
  }
}
