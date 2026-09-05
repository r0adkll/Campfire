// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.appbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
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
import app.campfire.common.compose.LocalWindowChromeInsets
import app.campfire.common.compose.extensions.plus
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Close
import app.campfire.common.compose.icons.rounded.Search
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.search.api.ui.SearchComponent
import campfire.ui.appbar.generated.resources.Res
import campfire.ui.appbar.generated.resources.action_clear_search
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
        Icon(CampfireIcons.Rounded.Search, contentDescription = null)
      },
      trailingIcon = {
        AnimatedVisibility(
          visible = textFieldState.text.isNotEmpty() ||
            searchBarState.currentValue == SearchBarValue.Expanded,
          enter = fadeIn(),
          exit = fadeOut(),
        ) {
          val clearLabel = stringResource(Res.string.action_clear_search)
          IconButtonTooltip(text = clearLabel) {
            IconButton(
              onClick = {
                textFieldState.clearText()
                scope.launch { searchBarState.animateToCollapsed() }
              },
            ) {
              Icon(CampfireIcons.Rounded.Close, contentDescription = clearLabel)
            }
          }
        }
      },
      placeholder = {
        Text(stringResource(Res.string.search_placeholder_text))
      },
      modifier = Modifier.fillMaxWidth(),
    )
  }

  AppBarWithSearch(
    state = searchBarState,
    inputField = inputField,
    scrollBehavior = scrollBehavior,
    modifier = modifier.fillMaxWidth(),
    windowInsets = WindowInsets(),
    contentPadding = SearchBarDefaults.windowInsets
      .add(LocalWindowChromeInsets.current)
      .only(WindowInsetsSides.Top)
      .asPaddingValues() + PaddingValues(horizontal = 8.dp),
  )

  // The expanded sheet is a popup anchored to the collapsed pill's top-left corner and sized
  // to the pill's width. Forcing a wider size here makes it grow past the pill's right edge,
  // so leave the width to the search bar state.
  ExpandedDockedSearchBar(
    state = searchBarState,
    inputField = inputField,
  ) {
    resultContent()
  }
}
