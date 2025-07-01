package app.campfire.search.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.campfire.audioplayer.offline.asWidgetStatus
import app.campfire.common.compose.di.rememberComponent
import app.campfire.search.ui.composables.SearchResultContent
import app.campfire.search.ui.di.SearchUiComponent
import campfire.features.search.ui.generated.resources.Res
import campfire.features.search.ui.generated.resources.search_placeholder_format
import campfire.features.search.ui.generated.resources.search_placeholder_options
import com.slack.circuit.runtime.Navigator
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun CampfireDockedSearchBar(
  navigator: Navigator,
  modifier: Modifier = Modifier,
  component: SearchUiComponent = rememberComponent(),
) {
  var expanded by remember { mutableStateOf(false) }

  val presenter = remember(component, navigator) { component.searchPresenterFactory(navigator) { expanded = false } }
  val uiState = presenter.present()

  CampfireDockedSearchBar(
    state = uiState,
    expanded = expanded,
    onExpandedChange = { expanded = it },
    modifier = modifier,
  )
}

@Composable
private fun CampfireDockedSearchBar(
  state: SearchUiState,
  expanded: Boolean,
  onExpandedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  val eventSink = state.eventSink

  DockedSearchBar(
    inputField = {
      SearchBarDefaults.InputField(
        query = state.query,
        onQueryChange = {
          eventSink(SearchUiEvent.QueryChanged(it))
        },
        onSearch = {
          eventSink(SearchUiEvent.QueryChanged(it))
        },
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        leadingIcon = {
          Icon(Icons.Rounded.Search, contentDescription = null)
        },
        trailingIcon = {
          AnimatedVisibility(
            visible = state.query.isNotBlank() || expanded,
            enter = fadeIn(),
            exit = fadeOut(),
          ) {
            IconButton(
              onClick = {
                eventSink(SearchUiEvent.ClearQuery)
                eventSink(SearchUiEvent.Dismiss)
                onExpandedChange(false)
              },
            ) {
              Icon(Icons.Rounded.Clear, contentDescription = null)
            }
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
        modifier = Modifier.fillMaxWidth(),
      )
    },
    expanded = expanded,
    onExpandedChange = onExpandedChange,
    modifier = modifier,
  ) {
    SearchResultContent(
      query = state.query,
      results = state.searchResult,
      offlineStatus = { state.offlineStates[it].asWidgetStatus() },
      onBookClick = { book -> eventSink(SearchUiEvent.OnBookClick(book)) },
      onNarratorClick = { narrator -> eventSink(SearchUiEvent.OnNarratorClick(narrator)) },
      onAuthorClick = { author -> eventSink(SearchUiEvent.OnAuthorClick(author)) },
      onSeriesClick = { series -> eventSink(SearchUiEvent.OnSeriesClick(series)) },
      onTagClick = { tag -> eventSink(SearchUiEvent.OnTagClick(tag)) },
      onGenreClick = { genre -> eventSink(SearchUiEvent.OnGenreClick(genre)) },
    )
  }
}
