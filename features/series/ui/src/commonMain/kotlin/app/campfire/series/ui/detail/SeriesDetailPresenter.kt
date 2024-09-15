package app.campfire.series.ui.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.campfire.common.screens.LibraryItemScreen
import app.campfire.common.screens.SeriesDetailScreen
import app.campfire.core.di.UserScope
import app.campfire.series.api.SeriesRepository
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@CircuitInject(SeriesDetailScreen::class, UserScope::class)
@Inject
class SeriesDetailPresenter(
  @Assisted private val screen: SeriesDetailScreen,
  @Assisted private val navigator: Navigator,
  private val repository: SeriesRepository,
) : Presenter<SeriesDetailUiState> {

  @Composable
  override fun present(): SeriesDetailUiState {
    val seriesContentState by remember {
      repository.observeSeriesLibraryItems(seriesId = screen.seriesId)
        .map { SeriesContentState.Loaded(it) }
        .catch { SeriesContentState.Error }
    }.collectAsState(SeriesContentState.Loading)

    return SeriesDetailUiState(
      seriesContentState = seriesContentState,
    ) { event ->
      when (event) {
        SeriesDetailUiEvent.Back -> navigator.pop()
        is SeriesDetailUiEvent.LibraryItemClick -> navigator.goTo(LibraryItemScreen(event.libraryItem.id))
      }
    }
  }
}
