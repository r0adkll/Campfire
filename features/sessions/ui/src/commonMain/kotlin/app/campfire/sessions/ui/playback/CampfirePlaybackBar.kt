package app.campfire.sessions.ui.playback

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.layout.isLandscapePhone
import app.campfire.common.compose.theme.colorScheme
import app.campfire.core.di.ComponentHolder
import app.campfire.core.di.UserScope
import app.campfire.core.extensions.fluentIf
import app.campfire.sessions.ui.playback.PlaybackBarState.Collapsed
import app.campfire.sessions.ui.playback.PlaybackBarState.Expanded
import app.campfire.sessions.ui.playback.PlaybackBarState.Hidden
import app.campfire.sessions.ui.playback.collapsed.CollapsedPlaybackBar
import app.campfire.sessions.ui.playback.expanded.ExpandedPlaybackBar
import app.campfire.sessions.ui.playback.expanded.SmallExpandedPlaybackBar
import com.r0adkll.kimchi.annotations.ContributesTo
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.Navigator

@ContributesTo(UserScope::class)
interface CampfirePlaybackBarComponent {
  val playbackPresenterFactory: PlaybackPresenterFactory
}

@Composable
private fun rememberCampfirePlaybackBarComponent(): State<CampfirePlaybackBarComponent> {
  return remember {
    ComponentHolder.subscribe<CampfirePlaybackBarComponent>()
  }.collectAsState(ComponentHolder.component<CampfirePlaybackBarComponent>())
}

@Composable
fun CampfirePlaybackBar(
  enabled: Boolean,
  expanded: Boolean,
  onExpansionChange: (Boolean) -> Unit,
  offset: Density.() -> IntOffset,
  navigator: Navigator,
  modifier: Modifier = Modifier,
  component: State<CampfirePlaybackBarComponent> = rememberCampfirePlaybackBarComponent(),
) {
  val comp by component
  key(comp) {
    val presenter = rememberRetained {
      comp.playbackPresenterFactory()
    }

    val uiState = presenter.present(expanded)

    CampfirePlaybackBar(
      enabled = enabled,
      expanded = expanded,
      onExpansionChange = onExpansionChange,
      offset = offset,
      uiState = uiState,
      navigator = navigator,
      modifier = modifier,
    )
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CampfirePlaybackBar(
  enabled: Boolean,
  expanded: Boolean,
  onExpansionChange: (Boolean) -> Unit,
  offset: Density.() -> IntOffset,
  uiState: PlaybackUiState,
  navigator: Navigator,
  modifier: Modifier = Modifier,
) = SharedTransitionLayout(modifier) {
  val windowSizeClass = LocalWindowSizeClass.current

  MaterialExpressiveTheme(
    colorScheme = uiState.themeState.theme?.colorScheme,
  ) {
    val sheetContainerColor = if (uiState.themeState.dynamicThemingEnabled) {
      DefaultSheetColor
    } else {
      DefaultNonThemedSheetColor
    }

    val sheetContentColor = if (uiState.themeState.dynamicThemingEnabled) {
      DefaultSheetContentColor
    } else {
      DefaultNonThemedContentColor
    }

    AnimatedContent(
      targetState = when {
        !enabled -> Hidden
        uiState.session == null -> Hidden
        expanded -> Expanded
        else -> Collapsed
      },
      transitionSpec = {
        when {
          (initialState == Hidden && targetState == Collapsed) ||
            (initialState == Collapsed && targetState == Hidden)
          -> slideInVertically { it } togetherWith slideOutVertically { it }

          else -> scaleIn() togetherWith scaleOut()
        }
      },
    ) { state ->
      val scope = CampfirePlaybackBarScope(
        sharedTransitionScope = this@SharedTransitionLayout,
        animatedContentScope = this,
      )

      when (state) {
        Hidden -> Unit
        Collapsed -> scope.CollapsedPlaybackBar(
          session = uiState.session,
          playerState = uiState.playerState,
          syncState = uiState.syncUiState,
          containerColor = sheetContainerColor,
          contentColor = sheetContentColor,
          onClick = { onExpansionChange(!expanded) },
          onDispose = { uiState.eventSink(PlaybackUiEvent.ClearSession) },
          modifier = Modifier
            .padding(8.dp)
            .offset(offset)
            .fluentIf(windowSizeClass.isLandscapePhone) {
              widthIn(max = 500.dp)
            },
        )
        Expanded -> if (windowSizeClass.isLandscapePhone) {
          scope.SmallExpandedPlaybackBar(
            navigator = navigator,
            session = uiState.session,
            playbackState = uiState,
            containerColor = sheetContainerColor,
            contentColor = sheetContentColor,
            onClose = { onExpansionChange(false) },
            modifier = Modifier.fillMaxSize(),
          )
        } else {
          scope.ExpandedPlaybackBar(
            navigator = navigator,
            session = uiState.session,
            playbackState = uiState,
            containerColor = sheetContainerColor,
            contentColor = sheetContentColor,
            onClose = { onExpansionChange(false) },
            modifier = Modifier.fillMaxSize(),
          )
        }
      }
    }
  }
}

class CampfirePlaybackBarScope(
  sharedTransitionScope: SharedTransitionScope,
  animatedContentScope: AnimatedContentScope,
) : SharedTransitionScope by sharedTransitionScope,
  AnimatedVisibilityScope by animatedContentScope

private enum class PlaybackBarState {
  Hidden,
  Collapsed,
  Expanded,
}

internal val DefaultSheetColor: Color
  @Composable get() = MaterialTheme.colorScheme.surfaceContainerHighest
internal val DefaultSheetContentColor: Color
  @Composable get() = MaterialTheme.colorScheme.onSurface

internal val DefaultNonThemedSheetColor: Color
  @Composable get() = MaterialTheme.colorScheme.secondaryContainer
internal val DefaultNonThemedContentColor: Color
  @Composable get() = MaterialTheme.colorScheme.onSecondaryContainer

internal const val SharedBounds = "bounds"
internal const val SharedImage = "image"
