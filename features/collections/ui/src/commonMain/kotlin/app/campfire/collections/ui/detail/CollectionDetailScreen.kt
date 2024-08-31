package app.campfire.collections.ui.detail

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.campfire.common.screens.CollectionDetailScreen
import app.campfire.core.di.UserScope
import com.r0adkll.kimchi.circuit.annotations.CircuitInject

@CircuitInject(CollectionDetailScreen::class, UserScope::class)
@Composable
fun CollectionDetail(
  state: CollectionDetailUiState,
  modifier: Modifier = Modifier,
) {
  TODO("Add screen compose code")
}
