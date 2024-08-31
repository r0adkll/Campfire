package app.campfire.common.compose.widgets

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.campfire.common.compose.theme.CampfireTheme

@Preview
@Composable
fun EmptyStatePreview() {
  CampfireTheme {
    Surface {
      EmptyState(
        message = "This is a preview of an empty state. Here messages about missing content will show.",
      )
    }
  }
}
