// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.appbar

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SearchBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.layout.NavigationType
import app.campfire.common.compose.layout.isLandscapePhone
import app.campfire.common.compose.layout.isSupportingPaneEnabled
import app.campfire.common.compose.layout.navigationType
import app.campfire.common.compose.navigation.localDrawerOpener
import app.campfire.core.di.UserScope
import app.campfire.core.model.Library
import app.campfire.libraries.api.LibraryRepository
import app.campfire.search.api.ui.SearchComponent
import app.campfire.ui.theming.api.widgets.ThemeIconContent
import app.campfire.updates.AppUpdates
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import dev.zacsweers.metro.ContributesBinding
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * The app bar shown atop top-level screens, injected into Circuit UIs that need it.
 */
fun interface CampfireAppBar {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  operator fun invoke(
    modifier: Modifier,
    scrollBehavior: SearchBarScrollBehavior?,
  )
}

@ContributesBinding(UserScope::class)
class DefaultCampfireAppBar(
  private val searchComponent: SearchComponent,
  private val themeIconContent: ThemeIconContent,
  private val appUpdates: AppUpdates,
  private val libraryRepository: LibraryRepository,
) : CampfireAppBar {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun invoke(
    modifier: Modifier,
    scrollBehavior: SearchBarScrollBehavior?,
  ) {
    // The drawer's update card waits behind the menu button
    val update by appUpdates.undismissedUpdate.collectAsState()

    // Cached reads: every top-level screen hosts an app bar, and the drawer already refreshes these
    val scope = rememberCoroutineScope()
    val currentLibrary by remember {
      libraryRepository.observeCurrentLibrary(refresh = false)
        .map<Library, Library?> { it }
        .catch { emit(null) }
    }.collectAsState(null)
    val libraries by remember {
      libraryRepository.observeAllLibraries(refresh = false)
        .map { all -> all.sortedBy { it.displayOrder } }
        .catch { emit(emptyList()) }
    }.collectAsState(emptyList())
    val librarySwitcher = remember(currentLibrary, libraries) {
      librarySwitcherModel(currentLibrary, libraries) { library ->
        scope.launch { libraryRepository.setCurrentLibrary(library) }
      }
    }

    CampfireAppBarContent(
      searchComponent = searchComponent,
      themeIconContent = themeIconContent,
      librarySwitcher = librarySwitcher,
      menuBadged = update != null,
      modifier = modifier,
      scrollBehavior = scrollBehavior,
    )
  }
}

object SharedAppBar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun CampfireAppBarContent(
  searchComponent: SearchComponent,
  themeIconContent: ThemeIconContent,
  librarySwitcher: LibrarySwitcherModel?,
  menuBadged: Boolean,
  modifier: Modifier,
  scrollBehavior: SearchBarScrollBehavior?,
) = SharedElementTransitionScope {
  val windowSizeClass by rememberUpdatedState(LocalWindowSizeClass.current)
  if (windowSizeClass.isSupportingPaneEnabled && !windowSizeClass.isLandscapePhone) {
    ExpandedCampfireAppBar(
      searchComponent = searchComponent,
      // The wide rail and the permanent drawer already show the library picker
      librarySwitcher = librarySwitcher.takeIf { windowSizeClass.navigationType == NavigationType.Rail },
      scrollBehavior = scrollBehavior,
      modifier = modifier
        .sharedElement(
          sharedContentState = rememberSharedContentState(SharedAppBar),
          animatedVisibilityScope = requireAnimatedScope(SharedElementTransitionScope.AnimatedScope.Navigation),
        ),
    )
  } else {
    CompactCampfireAppBar(
      searchComponent = searchComponent,
      themeIconContent = themeIconContent,
      themeIconEnabled = !windowSizeClass.isLandscapePhone,
      librarySwitcher = librarySwitcher,
      menuBadged = menuBadged,
      scrollBehavior = scrollBehavior,
      modifier = modifier
        .sharedElement(
          sharedContentState = rememberSharedContentState(SharedAppBar),
          animatedVisibilityScope = requireAnimatedScope(SharedElementTransitionScope.AnimatedScope.Navigation),
        ),
    )
  }
}

@Composable
private fun CompactCampfireAppBar(
  searchComponent: SearchComponent,
  themeIconContent: ThemeIconContent,
  themeIconEnabled: Boolean,
  librarySwitcher: LibrarySwitcherModel?,
  menuBadged: Boolean,
  modifier: Modifier = Modifier,
  scrollBehavior: SearchBarScrollBehavior? = null,
) {
  val drawerOpener = localDrawerOpener()
  CampfireSearchAppBar(
    searchComponent = searchComponent,
    themeIconContent = themeIconContent,
    themeIconEnabled = themeIconEnabled,
    librarySwitcher = librarySwitcher,
    themeIconBadged = menuBadged,
    onNavigationClick = drawerOpener,
    modifier = modifier,
    scrollBehavior = scrollBehavior,
  )
}

@Composable
private fun ExpandedCampfireAppBar(
  searchComponent: SearchComponent,
  librarySwitcher: LibrarySwitcherModel?,
  modifier: Modifier = Modifier,
  scrollBehavior: SearchBarScrollBehavior? = null,
) {
  CampfireDockedSearchBar(
    searchComponent = searchComponent,
    librarySwitcher = librarySwitcher,
    scrollBehavior = scrollBehavior,
    modifier = modifier,
  )
}
