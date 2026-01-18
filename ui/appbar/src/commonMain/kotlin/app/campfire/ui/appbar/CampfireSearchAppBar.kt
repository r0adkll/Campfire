package app.campfire.ui.appbar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarScrollBehavior
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.extensions.plus
import app.campfire.search.api.ui.SearchComponent
import app.campfire.ui.theming.api.widgets.ThemeIconContent
import campfire.ui.appbar.generated.resources.Res
import campfire.ui.appbar.generated.resources.search_placeholder_text
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CampfireSearchAppBar(
  searchComponent: SearchComponent,
  themeIconContent: ThemeIconContent,
  onNavigationClick: () -> Unit,
  modifier: Modifier = Modifier,
  actions: @Composable (RowScope.() -> Unit)? = null,
  scrollBehavior: SearchBarScrollBehavior? = null,
) {
  CampfireSearchAppBar(
    searchComponent = searchComponent,
    navigationIcon = {
      themeIconContent.Content(
        onClick = onNavigationClick,
        modifier = Modifier
          .size(40.dp)
          .padding(4.dp),
      )
    },
    actions = actions,
    modifier = modifier,
    scrollBehavior = scrollBehavior,
  )
}

/**
 * The root appbar for top-level screens to re-use to provide a consistent UI experience across
 * their surfaces.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CampfireSearchAppBar(
  searchComponent: SearchComponent,
  navigationIcon: @Composable () -> Unit,
  modifier: Modifier = Modifier,
  actions: @Composable (RowScope.() -> Unit)? = null,
  scrollBehavior: SearchBarScrollBehavior? = null,
) {
  val scope = rememberCoroutineScope()
  val textFieldState = rememberTextFieldState()
  val searchBarState = rememberSearchBarState()
  val inputField =
    @Composable {
      SearchBarDefaults.InputField(
        modifier = Modifier.fillMaxWidth(),
        searchBarState = searchBarState,
        textFieldState = textFieldState,
        onSearch = { scope.launch { searchBarState.animateToCollapsed() } },
        placeholder = {
          Text(
            modifier = Modifier.clearAndSetSemantics {},
            text = stringResource(Res.string.search_placeholder_text),
          )
        },
        leadingIcon = {
          AnimatedContent(
            targetState = searchBarState.currentValue,
            contentAlignment = Alignment.Center,
          ) { state ->
            when (state) {
              SearchBarValue.Expanded -> {
                IconButton(
                  onClick = { scope.launch { searchBarState.animateToCollapsed() } },
                ) {
                  Icon(
                    Icons.AutoMirrored.Default.ArrowBack,
                    contentDescription = "Back",
                  )
                }
              }
              SearchBarValue.Collapsed -> {
                Icon(Icons.Default.Search, contentDescription = null)
              }
            }
          }
        },
        trailingIcon = {
          AnimatedVisibility(
            visible = searchBarState.currentValue == SearchBarValue.Expanded,
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
      )
    }

  AppBarWithSearch(
    state = searchBarState,
    inputField = inputField,
    modifier = modifier,
    navigationIcon = navigationIcon,
    actions = actions,
    scrollBehavior = scrollBehavior,
    windowInsets = SearchBarDefaults.windowInsets
      .only(WindowInsetsSides.Horizontal),
    contentPadding = SearchBarDefaults.windowInsets
      .only(WindowInsetsSides.Top)
      .asPaddingValues() + PaddingValues(horizontal = 8.dp),
  )

  // FIXME: HACK to workaround issue where the search bar steals focus from other input fields
  //  See: https://issuetracker.google.com/u/1/issues/457791152

  val density = LocalDensity.current
  val ime = WindowInsets.ime
  val isImeVisible by remember(ime, density) { derivedStateOf { ime.getBottom(density) > 0 } }
  var addSearchDialogToComposition by remember { mutableStateOf(false) }

  LaunchedEffect(searchBarState.currentValue, isImeVisible) {
    if (searchBarState.currentValue == SearchBarValue.Expanded) {
      addSearchDialogToComposition = true
    } else if (searchBarState.currentValue == SearchBarValue.Collapsed && !isImeVisible) {
      addSearchDialogToComposition = false
    }
  }

  if (addSearchDialogToComposition) {
    ExpandedFullScreenSearchBar(
      state = searchBarState,
      inputField = inputField,
    ) {
      searchComponent.ResultContent(
        textFieldState = textFieldState,
      )
    }
  }
}
