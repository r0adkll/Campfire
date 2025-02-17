package app.campfire.debug.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import app.campfire.common.screens.DebugScreen
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.coroutines.LoadState
import app.campfire.core.coroutines.map
import app.campfire.core.di.UserScope
import app.campfire.debug.events.LogEvent
import app.campfire.debug.events.storage.EventStorage
import app.campfire.debug.screen.model.EventUiModel
import app.campfire.debug.screen.model.LogEventProcessor
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@CircuitInject(DebugScreen::class, UserScope::class)
@Inject
class DebugPresenter(
  private val eventStorage: EventStorage,
  private val logEventProcessor: LogEventProcessor,
  private val dispatcherProvider: DispatcherProvider,
  @Assisted private val navigator: Navigator,
) : Presenter<DebugUiState> {

  @OptIn(ExperimentalCoroutinesApi::class)
  @Composable
  override fun present(): DebugUiState {
    var filter by rememberSaveable { mutableStateOf("") }

    val events by remember {
      eventStorage.observeAll()
        .mapLatest { logs -> LoadState.Loaded(processLogs(logs)) }
        .catch { LoadState.Error }
    }.collectAsState(LoadState.Loading)

    val filteredEvents by remember {
      derivedStateOf {
        events.map {
          it.filter { model -> model.message.contains(filter, ignoreCase = true) }.toImmutableList()
        }
      }
    }

    return DebugUiState(
      filter = filter,
      events = filteredEvents,
    ) { event ->
      when (event) {
        DebugUiEvent.Back -> navigator.pop()
        DebugUiEvent.ClearQuery -> filter = ""
        is DebugUiEvent.Query -> filter = event.query
      }
    }
  }

  private suspend fun processLogs(
    logs: List<LogEvent>,
  ): ImmutableList<EventUiModel> = withContext(dispatcherProvider.computation) {
    logs.map { logEventProcessor.process(it) }.toPersistentList()
  }
}
