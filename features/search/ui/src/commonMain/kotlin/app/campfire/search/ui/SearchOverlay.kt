package app.campfire.search.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import app.campfire.common.compose.di.rememberComponent
import app.campfire.search.ui.composables.SearchResultContent
import app.campfire.search.ui.di.SearchUiComponent
import campfire.features.search.ui.generated.resources.Res
import campfire.features.search.ui.generated.resources.search_placeholder_format
import campfire.features.search.ui.generated.resources.search_placeholder_options
import com.slack.circuit.overlay.Overlay
import com.slack.circuit.overlay.OverlayHost
import com.slack.circuit.overlay.OverlayNavigator
import com.slack.circuit.runtime.Navigator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource

suspend fun OverlayHost.showSearchOverlay(
  navigator: Navigator,
) {
  show(SearchOverlay(navigator))
}

class SearchOverlay(
  private val homeNavigator: Navigator,
) : Overlay<Unit> {

  @Composable
  override fun Content(navigator: OverlayNavigator<Unit>) {
    val component = rememberComponent<SearchUiComponent>()
    val presenter = remember(component) {
      component.searchPresenterFactory(homeNavigator) {
        navigator.finish(Unit)
      }
    }

    val uiState = presenter.present()
    SearchContent(uiState)
  }

  @Composable
  private fun SearchContent(
    state: SearchUiState,
  ) {
    val scope = rememberCoroutineScope()
    val eventSink = state.eventSink

    var entryVisibility by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
      entryVisibility = true
      delay(50)
      expanded = true
      delay(100)
    }

    val dismiss = {
      scope.launch {
        expanded = false
        delay(150)
        entryVisibility = false
        delay(200)
        eventSink(SearchUiEvent.Dismiss)
      }
    }

    Box(
      modifier = Modifier
        .fillMaxSize()
        .semantics { isTraversalGroup = true },
    ) {
      AnimatedVisibility(
        visible = entryVisibility,
        enter = fadeIn() + slideInHorizontally { it },
        exit = fadeOut() + slideOutHorizontally { it },
        modifier = Modifier
          .align(Alignment.TopCenter),
      ) {
        SearchBar(
          modifier = Modifier
            .semantics { traversalIndex = 0f },
          inputField = {
            SearchBarDefaults.InputField(
              modifier = Modifier.statusBarsPadding(),
              query = state.query,
              onQueryChange = { eventSink(SearchUiEvent.QueryChanged(it)) },
              onSearch = { eventSink(SearchUiEvent.QueryChanged(it)) },
              expanded = expanded,
              onExpandedChange = { },
              leadingIcon = {
                IconButton(
                  onClick = {
                    dismiss()
                  },
                ) {
                  Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                }
              },
              trailingIcon = {
                IconButton(
                  onClick = {
                    eventSink(SearchUiEvent.ClearQuery)
                  },
                ) {
                  Icon(Icons.Rounded.Clear, contentDescription = null)
                }
              },
              placeholder = {
                val options = stringArrayResource(Res.array.search_placeholder_options)
                val placeholderText = stringResource(
                  Res.string.search_placeholder_format,
                  options.random(),
                )
                Text(placeholderText)
              },
            )
          },
          expanded = expanded,
          onExpandedChange = { dismiss() },
        ) {
          CompositionLocalProvider(
            LocalContentColor provides contentColorFor(SearchBarDefaults.colors().containerColor),
          ) {
            SearchResultContent(
              modifier = Modifier.imePadding(),
              query = state.query,
              results = state.searchResult,
              onBookClick = { book -> eventSink(SearchUiEvent.OnBookClick(book)) },
              onNarratorClick = { narrator -> eventSink(SearchUiEvent.OnNarratorClick(narrator)) },
              onAuthorClick = { author -> eventSink(SearchUiEvent.OnAuthorClick(author)) },
              onSeriesClick = { series -> eventSink(SearchUiEvent.OnSeriesClick(series)) },
              onTagClick = { tag -> eventSink(SearchUiEvent.OnTagClick(tag)) },
              onGenreClick = { genre -> eventSink(SearchUiEvent.OnGenreClick(genre)) },
            )
          }
        }
      }
    }
  }
}
