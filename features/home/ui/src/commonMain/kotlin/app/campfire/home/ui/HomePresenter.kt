package app.campfire.home.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.campfire.common.screens.AuthorDetailScreen
import app.campfire.common.screens.HomeScreen
import app.campfire.common.screens.LibraryItemScreen
import app.campfire.common.screens.SeriesDetailScreen
import app.campfire.core.di.UserScope
import app.campfire.home.api.HomeRepository
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@CircuitInject(HomeScreen::class, UserScope::class)
@Inject
class HomePresenter(
  @Assisted private val navigator: Navigator,
  private val homeRepository: HomeRepository,
) : Presenter<HomeUiState> {

  @Composable
  override fun present(): HomeUiState {
    val feed by remember {
      homeRepository.observeHomeFeed()
        .map { HomeFeed.Loaded(it) }
        .catch { HomeFeed.Error }
    }.collectAsState(HomeFeed.Loading)

    return HomeUiState(
      homeFeed = feed,
    ) { event ->
      when (event) {
        is HomeUiEvent.OpenLibraryItem -> navigator.goTo(LibraryItemScreen(event.item.id))
        is HomeUiEvent.OpenAuthor -> navigator.goTo(AuthorDetailScreen(event.author.id, event.author.name))
        is HomeUiEvent.OpenSeries -> navigator.goTo(SeriesDetailScreen(event.series.id, event.series.name))
      }
    }
  }
}
