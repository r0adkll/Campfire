package app.campfire.ui.appbar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import app.campfire.account.api.ServerRepository
import app.campfire.common.compose.widgets.AppBarState
import app.campfire.common.compose.widgets.AppBarState.LibraryState
import app.campfire.common.compose.widgets.AppBarState.ServerState
import app.campfire.common.compose.widgets.AppBarViewEvent
import app.campfire.core.model.Library
import app.campfire.libraries.api.LibraryRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

@Inject
class CampfireAppbarPresenter(
  private val serverRepository: ServerRepository,
  private val libraryRepository: LibraryRepository,
) {

  @Composable
  fun present(): AppBarState {
    val scope = rememberCoroutineScope()

    val server by remember {
      serverRepository.observeCurrentServer()
    }.collectAsState(null)

    val library by remember {
      libraryRepository.observeCurrentLibrary()
        .catch { null }
    }.collectAsState(null)

    val libraries by remember {
      libraryRepository.observeAllLibraries()
        .map { it.sortedBy { it.displayOrder } }
        .catch { emptyList<Library>() }
    }.collectAsState(emptyList())

    // TODO: Observe this from some socket repository that can broadcast
    //  the socket connection state when that feature is built
    val connectionState by remember {
      mutableStateOf(AppBarState.ConnectionState.Connected)
    }

    return AppBarState(
      library = library?.let { LibraryState.Loaded(it) } ?: LibraryState.Loading,
      server = server?.let { ServerState.Loaded(it, connectionState) } ?: ServerState.Loading,
      allLibraries = libraries,
    ) { event ->
      when (event) {
        is AppBarViewEvent.LibrarySelected -> {
          scope.launch {
            libraryRepository.setCurrentLibrary(event.library)
          }
        }
      }
    }
  }
}
