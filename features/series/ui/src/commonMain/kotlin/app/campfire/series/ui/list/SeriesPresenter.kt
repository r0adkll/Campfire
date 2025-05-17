package app.campfire.series.ui.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.campfire.common.screens.SeriesDetailScreen
import app.campfire.common.screens.SeriesScreen
import app.campfire.core.di.UserScope
import app.campfire.series.api.SeriesRepository
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

@Inject
class SeriesPresenter(
  @Assisted private val navigator: Navigator,
  private val seriesRepository: SeriesRepository,
) : Presenter<SeriesUiState> {

  @CircuitInject(SeriesScreen::class, UserScope::class)
  @AssistedFactory
  fun interface Factory {
    fun create(navigator: Navigator): SeriesPresenter
  }

  @Composable
  override fun present(): SeriesUiState {
    val seriesContentState by remember {
      seriesRepository.observeAllSeries()
        .map { SeriesContentState.Loaded(it) }
        .catch { SeriesContentState.Error }
    }.collectAsState(SeriesContentState.Loading)

    return SeriesUiState(
      seriesContentState = seriesContentState,
    ) { event ->
      when (event) {
        is SeriesUiEvent.SeriesClicked -> navigator.goTo(SeriesDetailScreen(event.series.id, event.series.name))
      }
    }
  }
}
