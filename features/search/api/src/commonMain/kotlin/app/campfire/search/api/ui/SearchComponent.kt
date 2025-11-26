package app.campfire.search.api.ui

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.Screen

/**
 * An interface for providing composable search functionality
 * to other parts of the app
 */
interface SearchComponent {
  @Composable
  fun ResultContent(
    textFieldState: TextFieldState,
  )
}

val LocalSearchEventHandler = compositionLocalOf<(SearchResultNavEvent) -> Unit> {
  {
    // Do nothing by default.
  }
}

data class SearchResultNavEvent(
  val screen: Screen,
  val resetRoot: Boolean = false,
)

fun Navigator.goToSearchEvent(event: SearchResultNavEvent) {
  if (event.resetRoot) resetRoot(event.screen) else goTo(event.screen)
}
