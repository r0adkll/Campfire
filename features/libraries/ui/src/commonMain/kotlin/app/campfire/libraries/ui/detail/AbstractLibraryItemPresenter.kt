// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail

import androidx.compose.runtime.Composable
import app.campfire.core.model.LibraryItem
import app.campfire.libraries.api.screen.LibraryItemScreen
import com.slack.circuit.runtime.Navigator

interface AbstractLibraryItemPresenter {

  @Composable
  fun present(
    screen: LibraryItemScreen,
    navigator: Navigator,
    libraryItem: LibraryItem,
  ): ContentUiState
}
