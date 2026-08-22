// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.root.automation

import app.campfire.audioplayer.PlaybackController
import app.campfire.auth.api.AuthRepository
import app.campfire.common.screens.AuthorsScreen
import app.campfire.common.screens.CollectionsScreen
import app.campfire.common.screens.HomeScreen
import app.campfire.common.screens.SeriesScreen
import app.campfire.common.screens.SettingsScreen
import app.campfire.common.screens.StatisticsScreen
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.core.model.LibraryItemId
import app.campfire.core.navigation.DeepLink
import app.campfire.libraries.api.LibraryRepository
import app.campfire.libraries.api.screen.LibraryItemScreen
import app.campfire.libraries.api.screen.LibraryScreen
import app.campfire.playlists.api.screen.PlaylistsScreen
import app.campfire.sessions.api.SessionsRepository
import app.campfire.settings.api.CampfireSettings
import app.campfire.settings.api.ThemeMode
import app.campfire.ui.theming.api.AppTheme
import app.campfire.ui.theming.api.AppThemeRepository
import app.campfire.ui.theming.api.screen.ThemePickerScreen
import app.campfire.whatsnew.api.WhatsNewRepository
import com.slack.circuit.runtime.screen.Screen
import kotlinx.coroutines.flow.first
import me.tatarka.inject.annotations.Inject

/**
 * App-scoped handler for the debug-only automation deep links. These exist so the store screenshot
 * tool (`tools/screenshots/`) can put the app into a known state without driving the login and
 * settings UI, which would be brittle. See tools/screenshots/README.md ("Design decisions").
 */
@SingleIn(AppScope::class)
@Inject
class AutomationDeepLinks(
  private val authRepository: AuthRepository,
  private val settings: CampfireSettings,
  private val themeRepository: AppThemeRepository,
  private val whatsNewRepository: WhatsNewRepository,
) {

  /**
   * Sign in and silence every first-run prompt so the next composition lands on Home.
   * Safe to call when already signed in: only the settings are re-applied.
   */
  suspend fun applySetup(setup: DeepLink.Setup, isLoggedIn: Boolean) {
    settings.hasEverConsented = true
    settings.hasShownWidgetPinning = true
    whatsNewRepository.dismissWhatsNew()

    setup.themeMode?.let { settings.themeMode = ThemeMode.fromStorageKey(it) }
    setup.theme?.let { name ->
      fixedThemes.firstOrNull { it.id.equals(name, ignoreCase = true) }
        ?.let(themeRepository::setCurrentTheme)
        ?: bark(LogPriority.WARN) { "Automation setup: unknown theme '$name'" }
    }

    if (!isLoggedIn) {
      authRepository.authenticate(
        serverUrl = setup.serverUrl,
        serverName = setup.serverName,
        username = setup.username,
        password = setup.password,
      ).onFailure { bark(LogPriority.ERROR, throwable = it) { "Automation setup: authentication failed" } }
    }
  }

  private val fixedThemes: List<AppTheme.Fixed> = listOf(
    AppTheme.Fixed.Tent,
    AppTheme.Fixed.Rucksack,
    AppTheme.Fixed.WaterBottle,
    AppTheme.Fixed.Forest,
    AppTheme.Fixed.Mountain,
    AppTheme.Fixed.LifeFloat,
  )
}

/** User-scoped half of the automation deep links: library selection and playback. */
@SingleIn(UserScope::class)
@Inject
class UserAutomationDeepLinks(
  private val libraryRepository: LibraryRepository,
  private val playbackController: PlaybackController,
  private val sessionsRepository: SessionsRepository,
) {

  /** Select the library named [libraryName] (case-insensitive) for the signed-in user. */
  suspend fun selectLibrary(libraryName: String) {
    val libraries = libraryRepository.observeAllLibraries().first()
    val library = libraries.firstOrNull { it.name.equals(libraryName, ignoreCase = true) }
    if (library == null) {
      bark(LogPriority.WARN) { "Automation setup: no library named '$libraryName' in ${libraries.map { it.name }}" }
      return
    }
    libraryRepository.setCurrentLibrary(library)
  }

  fun play(libraryItemId: LibraryItemId) {
    playbackController.startSession(libraryItemId)
  }

  suspend fun stopPlayback() {
    val session = sessionsRepository.getCurrentSession() ?: return
    playbackController.stopSession(
      itemId = session.libraryItem.id,
      clearQueue = true,
      episodeId = session.episodeId,
    )
  }
}

/** Pure mapping from [DeepLink.Navigate] names to screens. */
object AutomationScreens {

  /** Resolve a [DeepLink.Navigate] to a screen. Returns `null` for unknown names. */
  fun resolve(navigate: DeepLink.Navigate): Screen? = when (navigate.screen) {
    "home" -> HomeScreen
    "library" -> LibraryScreen()
    "series" -> SeriesScreen
    "authors" -> AuthorsScreen
    "collections" -> CollectionsScreen
    "playlists" -> PlaylistsScreen
    "statistics" -> StatisticsScreen
    "theme_picker" -> ThemePickerScreen
    "settings" -> SettingsScreen(
      page = navigate.arg
        ?.let { arg -> SettingsScreen.Page.entries.firstOrNull { it.name.equals(arg, ignoreCase = true) } }
        ?: SettingsScreen.Page.Root,
    )
    "library_item" -> navigate.arg?.let { LibraryItemScreen(it) }
    else -> null
  }

  /** Screens that replace the root back stack rather than being pushed onto it. */
  val rootScreenNames = setOf("home", "library", "series", "authors", "collections", "playlists")
}
