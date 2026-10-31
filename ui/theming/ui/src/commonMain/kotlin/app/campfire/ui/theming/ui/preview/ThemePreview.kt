// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

@file:OptIn(ExperimentalSharedTransitionApi::class)

package app.campfire.ui.theming.ui.preview

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.layout.ContentLayout
import app.campfire.common.compose.layout.LocalContentLayout
import app.campfire.ui.theming.api.AppTheme
import com.slack.circuit.sharedelements.PreviewSharedElementTransitionLayout

/** A Campfire screen the theme preview can draw. */
enum class PreviewScene {
  Home,
  Library,

  /** The parts every screen shares: settings rows and switches, a dialog, banners, a snackbar. */
  Everywhere,

  /** Shown as a note while book details use cover colors. */
  Book,

  /** Shown as a note while the player uses cover colors. */
  Player,
}

/**
 * Campfire's screens drawn in the surrounding [MaterialTheme] color scheme, for previewing a
 * theme. Home and Library show the user's own books ([content]) in the app's own cards; the
 * search bar, navigation bar, mini player, book details and player are copies, as the real ones
 * need sessions and presenters.
 *
 * Book details and the player take their colors from book covers unless the user has turned
 * that off, so while they do, those scenes say so and offer [onCoverThemingClick] to change it.
 *
 * Each scene scrolls inside the space it's given, so give this a bounded height.
 *
 * @param icon the theme's icon, shown at the start of the search bar as in the app.
 */
@Composable
fun ThemePreview(
  scene: PreviewScene,
  content: PreviewContent,
  icon: AppTheme.Icon,
  modifier: Modifier = Modifier,
  onCoverThemingClick: (() -> Unit)? = null,
) {
  ThemePreviewContent(
    scene = scene,
    content = content,
    icon = icon,
    targets = ThemeTargets.None,
    onCoverThemingClick = onCoverThemingClick,
    modifier = modifier,
  )
}

@Composable
internal fun ThemePreviewContent(
  scene: PreviewScene,
  content: PreviewContent,
  icon: AppTheme.Icon,
  targets: ThemeTargets,
  modifier: Modifier = Modifier,
  onCoverThemingClick: (() -> Unit)? = null,
) {
  Surface(
    modifier = modifier,
    shape = MaterialTheme.shapes.extraLarge,
    color = MaterialTheme.colorScheme.surface,
    contentColor = MaterialTheme.colorScheme.onSurface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    // The preview's cards are the app's own, so they get a transition scope of their own rather
    // than joining the screen's, and the elevation of a top-level screen.
    PreviewSharedElementTransitionLayout {
      CompositionLocalProvider(LocalContentLayout provides ContentLayout.Root) {
        val book = content.library.nowPlaying?.item ?: PreviewLibrary.Samples.nowPlaying!!.item
        when (scene) {
          PreviewScene.Home -> HomeScene(content, icon, targets, onCoverThemingClick)
          PreviewScene.Library -> LibraryScene(content, icon, targets, onCoverThemingClick)
          PreviewScene.Everywhere -> EverywhereScene(targets)
          PreviewScene.Book -> if (content.coverTheming.bookDetails) {
            CoverColorsNote(CoverColorsNoteKind.BookDetails, onCoverThemingClick)
          } else {
            BookScene(book, targets)
          }

          PreviewScene.Player -> if (content.coverTheming.player) {
            CoverColorsNote(CoverColorsNoteKind.Player, onCoverThemingClick)
          } else {
            PlayerScene(book, targets)
          }
        }
      }
    }
  }
}
