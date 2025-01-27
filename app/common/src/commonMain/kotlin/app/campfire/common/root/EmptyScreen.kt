package app.campfire.common.root

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.campfire.common.compose.widgets.EmptyState
import app.campfire.common.screens.EmptyScreen
import app.campfire.core.di.UserScope
import com.r0adkll.kimchi.circuit.annotations.CircuitInject

@CircuitInject(EmptyScreen::class, UserScope::class)
@Composable
fun EmptyScreen(
  screen: EmptyScreen,
  modifier: Modifier = Modifier,
) {
  EmptyState(
    message = screen.message,
    modifier = modifier.fillMaxSize(),
  )
}
