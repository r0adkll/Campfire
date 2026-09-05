// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.attribution

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import app.campfire.common.compose.CampfireWindowInsets
import app.campfire.common.compose.widgets.CampfireTopAppBar
import app.campfire.common.compose.widgets.EmptyState
import app.campfire.common.compose.widgets.LoadingState
import app.campfire.common.compose.widgets.NavigationBackButton
import app.campfire.common.screens.AttributionScreen
import app.campfire.core.coroutines.LoadState
import app.campfire.core.di.UserScope
import campfire.ui.attribution.generated.resources.Res
import campfire.ui.attribution.generated.resources.attributions_error_message
import campfire.ui.attribution.generated.resources.attributions_title
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import org.jetbrains.compose.resources.stringResource

@CircuitInject(AttributionScreen::class, UserScope::class)
@Composable
fun Attribution(
  state: AttributionUiState,
  modifier: Modifier = Modifier,
) {
  val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
  Scaffold(
    topBar = {
      CampfireTopAppBar(
        scrollBehavior = scrollBehavior,
        title = { Text(stringResource(Res.string.attributions_title)) },
        navigationIcon = {
          NavigationBackButton(onClick = { state.eventSink(AttributionUiEvent.Back) })
        },
      )
    },
    contentWindowInsets = CampfireWindowInsets,
    modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
  ) { paddingValues ->
    when (state.attributionState) {
      LoadState.Loading -> LoadingState(Modifier.fillMaxSize())
      LoadState.Error -> EmptyState(stringResource(Res.string.attributions_error_message))
      is LoadState.Loaded -> LoadedContent(
        libs = state.attributionState.data,
        contentPadding = paddingValues,
      )
    }
  }
}

@Composable
private fun LoadedContent(
  libs: Libs,
  contentPadding: PaddingValues,
  modifier: Modifier = Modifier,
) {
  LibrariesContainer(
    libraries = libs,
    contentPadding = contentPadding,
    modifier = modifier.fillMaxSize(),
  )
}
