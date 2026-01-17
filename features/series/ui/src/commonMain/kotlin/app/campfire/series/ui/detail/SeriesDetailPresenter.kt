package app.campfire.series.ui.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.ContentSelected
import app.campfire.analytics.events.ContentType
import app.campfire.audioplayer.offline.OfflineDownloadManager
import app.campfire.common.screens.SeriesDetailScreen
import app.campfire.core.coroutines.LoadState
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItem
import app.campfire.libraries.api.screen.LibraryItemScreen
import app.campfire.series.api.SeriesRepository
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.foundation.NonPausablePresenter
import com.slack.circuit.runtime.Navigator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@CircuitInject(SeriesDetailScreen::class, UserScope::class)
@Inject
class SeriesDetailPresenter(
  @Assisted private val screen: SeriesDetailScreen,
  @Assisted private val navigator: Navigator,
  private val repository: SeriesRepository,
  private val offlineDownloadManager: OfflineDownloadManager,
  private val analytics: Analytics,
) : NonPausablePresenter<SeriesDetailUiState> {

  @Suppress("UNCHECKED_CAST")
  @OptIn(ExperimentalCoroutinesApi::class)
  @Composable
  override fun present(): SeriesDetailUiState {
    val seriesContentState by remember {
      repository.observeSeriesLibraryItems(seriesId = screen.seriesId)
        .map { LoadState.Loaded(it) as LoadState<List<LibraryItem>> }
        .catch { emit(LoadState.Error as LoadState<List<LibraryItem>>) }
    }.collectAsState(LoadState.Loading)

    val offlineDownloads by remember {
      snapshotFlow { seriesContentState.dataOrNull }
        .filterNotNull()
        .flatMapLatest { items ->
          offlineDownloadManager.observeForItems(items)
        }
    }.collectAsState(emptyMap())

    return SeriesDetailUiState(
      seriesContentState = seriesContentState,
      offlineStates = offlineDownloads,
    ) { event ->
      when (event) {
        SeriesDetailUiEvent.Back -> navigator.pop()
        is SeriesDetailUiEvent.LibraryItemClick -> {
          analytics.send(ContentSelected(ContentType.LibraryItem))
          navigator.goTo(
            LibraryItemScreen(
              libraryItemId = event.libraryItem.id,
              sharedTransitionKey = event.libraryItem.id + screen.seriesName,
            ),
          )
        }
      }
    }
  }
}
