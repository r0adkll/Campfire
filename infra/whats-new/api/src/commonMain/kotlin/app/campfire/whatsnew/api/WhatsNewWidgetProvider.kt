package app.campfire.whatsnew.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface WhatsNewWidgetProvider {

  @Composable
  fun Content(
    onClick: () -> Unit,
    modifier: Modifier,
  )
}
