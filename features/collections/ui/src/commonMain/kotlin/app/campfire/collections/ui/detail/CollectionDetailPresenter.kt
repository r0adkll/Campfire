package app.campfire.collections.ui.detail

import androidx.compose.runtime.Composable
import app.campfire.common.screens.CollectionDetailScreen
import app.campfire.core.di.UserScope
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@CircuitInject(CollectionDetailScreen::class, UserScope::class)
@Inject
class CollectionDetailPresenter(
  @Assisted private val screen: CollectionDetailScreen,
  @Assisted private val navigator: Navigator,
) : Presenter<CollectionDetailUiState> {

  @Composable
  override fun present(): CollectionDetailUiState {
    TODO("Not yet implemented")
  }
}
