package app.campfire.series.ui.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.campfire.common.screens.SeriesDetailScreen
import app.campfire.common.screens.SeriesScreen
import app.campfire.core.coroutines.LoadState
import app.campfire.core.di.UserScope
import app.campfire.series.api.SeriesRepository
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@CircuitInject(SeriesScreen::class, UserScope::class)
@Inject
class SeriesPresenter(
  @Assisted private val navigator: Navigator,
  private val seriesRepository: SeriesRepository,
) : Presenter<SeriesUiState> {

  @Composable
  override fun present(): SeriesUiState {
    val seriesContentState by remember {
      seriesRepository.observeAllSeries()
        .map { LoadState.Loaded(it) }
        .catch { LoadState.Error }
    }.collectAsState(LoadState.Loading)

    return SeriesUiState(
      seriesContentState = seriesContentState,
    ) { event ->
      when (event) {
        is SeriesUiEvent.SeriesClicked -> navigator.goTo(SeriesDetailScreen(event.series.id, event.series.name))
      }
    }
  }
}
