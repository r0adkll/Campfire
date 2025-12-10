package app.campfire.ui.theming.api.widgets

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface ThemeIconContent {

  @Composable
  fun Content(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
  )
}
