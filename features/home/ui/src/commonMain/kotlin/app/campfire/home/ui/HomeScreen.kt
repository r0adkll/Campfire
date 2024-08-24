package app.campfire.home.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.campfire.common.screens.HomeScreen
import app.campfire.core.di.UserScope
import com.r0adkll.kimchi.circuit.annotations.CircuitInject

@CircuitInject(HomeScreen::class, UserScope::class)
@Composable
fun HomeScreen(
  state: HomeUiState,
  modifier: Modifier = Modifier,
) {
  Text("Home")
}
