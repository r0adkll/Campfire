// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

@file:OptIn(ExperimentalSharedTransitionApi::class)

package app.campfire.ui.theming.ui.preview

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.layout.ContentLayout
import app.campfire.common.compose.layout.LocalContentLayout
import app.campfire.ui.theming.api.AppTheme
import app.campfire.ui.theming.ui.builder.composables.label
import campfire.ui.theming.ui.generated.resources.Res
import campfire.ui.theming.ui.generated.resources.theme_tune_part
import com.slack.circuit.sharedelements.PreviewSharedElementTransitionLayout
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource

/** A Campfire screen the theme preview can draw. */
enum class PreviewScene {
  Home,
  Library,

  /** The parts every screen shares: the drawer, settings switches, a dialog, a snackbar. */
  Everywhere,
}

/**
 * Campfire's components drawn in the surrounding [MaterialTheme] color scheme, for previewing a
 * theme: compact compositions of Home, Library and the parts every screen shares. Home and
 * Library show the user's own books ([content]) in the app's own cards; the search bar,
 * navigation bar and mini player are copies, as the real ones need sessions and presenters.
 *
 * The player takes its colors from book covers unless the user has turned that off, so while
 * it does, Home shows a note in the mini player's place and offers [onCoverThemingClick] to
 * change it.
 *
 * Each scene fits the space it's given, scaling down rather than scrolling.
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
        TappablePreviewParts(
          targets = targets,
          actionLabel = { stringResource(Res.string.theme_tune_part, it.label()) },
          modifier = Modifier.fillMaxSize(),
        ) {
          ScaledToFit(minSize = scene.minSize, modifier = Modifier.fillMaxSize()) {
            when (scene) {
              PreviewScene.Home -> HomeScene(content, icon, targets, onCoverThemingClick)
              PreviewScene.Library -> LibraryScene(content, targets)
              PreviewScene.Everywhere -> EverywhereScene(targets)
            }
          }
        }
      }
    }
  }
}

/**
 * The smallest space each scene is laid out in. In less, the whole scene scales down to fit
 * rather than scrolling or cutting parts off; in more, it gets the extra room.
 */
private val PreviewScene.minSize: DpSize
  get() = when (this) {
    PreviewScene.Home -> DpSize(360.dp, 470.dp)
    PreviewScene.Library -> DpSize(360.dp, 380.dp)
    PreviewScene.Everywhere -> DpSize(480.dp, 500.dp)
  }

/**
 * Lays [content] out at least [minSize], then scales it down uniformly to the space it has, so
 * a scene shows whole, in the app's proportions, however small the preview. Space beyond
 * [minSize] goes to the scene unscaled.
 */
@Composable
internal fun ScaledToFit(
  minSize: DpSize,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  Layout(content = content, modifier = modifier.clipToBounds()) { measurables, constraints ->
    val width = if (constraints.hasBoundedWidth) constraints.maxWidth else minSize.width.roundToPx()
    val height = if (constraints.hasBoundedHeight) constraints.maxHeight else minSize.height.roundToPx()
    val scale = minOf(1f, width / minSize.width.toPx(), height / minSize.height.toPx())
    val placeable = measurables.single().measure(
      Constraints.fixed((width / scale).roundToInt(), (height / scale).roundToInt()),
    )
    layout(width, height) {
      placeable.placeWithLayer(0, 0) {
        scaleX = scale
        scaleY = scale
        transformOrigin = TransformOrigin(0f, 0f)
      }
    }
  }
}
