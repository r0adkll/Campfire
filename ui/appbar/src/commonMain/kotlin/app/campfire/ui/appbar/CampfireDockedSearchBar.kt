package app.campfire.ui.appbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.ExpandedDockedSearchBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarScrollBehavior
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.extensions.plus
import app.campfire.search.api.ui.SearchComponent
import campfire.ui.appbar.generated.resources.Res
import campfire.ui.appbar.generated.resources.search_placeholder_text
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CampfireDockedSearchBar(
  searchComponent: SearchComponent,
  modifier: Modifier = Modifier,
  scrollBehavior: SearchBarScrollBehavior? = null,
) {
  val textFieldState = rememberTextFieldState()

  CampfireDockedSearchBar(
    textFieldState = textFieldState,
    scrollBehavior = scrollBehavior,
    modifier = modifier,
  ) {
    searchComponent.ResultContent(
      textFieldState = textFieldState,
    )
  }
}

@Composable
private fun CampfireDockedSearchBar(
  textFieldState: TextFieldState,
  modifier: Modifier = Modifier,
  scrollBehavior: SearchBarScrollBehavior? = null,
  resultContent: @Composable () -> Unit,
) {
  val scope = rememberCoroutineScope()
  val searchBarState = rememberSearchBarState()

  val inputField = @Composable {
    SearchBarDefaults.InputField(
      searchBarState = searchBarState,
      textFieldState = textFieldState,
      onSearch = { query ->
        // Do nothing
      },
      leadingIcon = {
        Icon(Icons.Rounded.Search, contentDescription = null)
      },
      trailingIcon = {
        AnimatedVisibility(
          visible = textFieldState.text.isNotEmpty() ||
            searchBarState.currentValue == SearchBarValue.Expanded,
          enter = fadeIn(),
          exit = fadeOut(),
        ) {
          IconButton(
            onClick = {
              textFieldState.clearText()
              scope.launch { searchBarState.animateToCollapsed() }
            },
          ) {
            Icon(Icons.Rounded.Clear, contentDescription = null)
          }
        }
      },
      placeholder = {
        Text(stringResource(Res.string.search_placeholder_text))
      },
      modifier = Modifier.fillMaxWidth(),
    )
  }

  BoxWithConstraints(
    modifier = modifier.fillMaxWidth(),
  ) {
    AppBarWithSearch(
      state = searchBarState,
      inputField = inputField,
      scrollBehavior = scrollBehavior,
      modifier = Modifier.fillMaxWidth(),
      windowInsets = SearchBarDefaults.windowInsets
        .only(WindowInsetsSides.Horizontal),
      contentPadding = SearchBarDefaults.windowInsets
        .only(WindowInsetsSides.Top)
        .asPaddingValues() + PaddingValues(horizontal = 8.dp),
    )

    ExpandedDockedSearchBar(
      state = searchBarState,
      inputField = inputField,
      modifier = Modifier
        .width(maxWidth - 32.dp),
    ) {
      resultContent()
    }
  }
}
