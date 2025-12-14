package app.campfire.sessions.ui.sheets.tracks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.campfire.analytics.events.ScreenType
import app.campfire.analytics.events.ScreenViewEvent
import app.campfire.common.compose.analytics.Impression
import app.campfire.common.compose.di.rememberComponent
import app.campfire.common.compose.extensions.clockFormat
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.BookRibbon
import app.campfire.core.di.UserScope
import app.campfire.core.extensions.fluentIf
import app.campfire.core.extensions.seconds
import app.campfire.core.model.AudioTrack
import app.campfire.sessions.ui.sheets.SessionSheetLayout
import app.campfire.sessions.ui.sheets.rememberSessionSheetTitleState
import app.campfire.settings.api.CampfireSettings
import campfire.features.sessions.ui.generated.resources.Res
import campfire.features.sessions.ui.generated.resources.audio_tracks_bottomsheet_title
import com.r0adkll.kimchi.annotations.ContributesTo
import com.slack.circuit.overlay.OverlayHost
import com.slack.circuitx.overlays.BottomSheetOverlay
import org.jetbrains.compose.resources.stringResource

sealed interface AudioTrackResult {
  data object None : AudioTrackResult
  data class Selected(val audioTrack: AudioTrack) : AudioTrackResult
}

@ContributesTo(UserScope::class)
interface AudioTrackListBottomSheetComponent {
  val settings: CampfireSettings
}

data class AudioTrackSheetModel(
  val audioTracks: List<AudioTrack>,
  val currentAudioTrack: AudioTrack? = null,
  val playbackSpeed: Float = 1f,
)

suspend fun OverlayHost.showAudioTrackBottomSheet(
  audioTracks: List<AudioTrack>,
  currentAudioTrack: AudioTrack? = null,
  playbackSpeed: Float = 1f,
): AudioTrackResult {
  return show(
    BottomSheetOverlay<AudioTrackSheetModel, AudioTrackResult>(
      model = AudioTrackSheetModel(
        audioTracks = audioTracks,
        currentAudioTrack = currentAudioTrack,
        playbackSpeed = playbackSpeed,
      ),
      onDismiss = { AudioTrackResult.None },
      sheetShape = RoundedCornerShape(
        topStart = 32.dp,
        topEnd = 32.dp,
      ),
      dragHandle = {
      },
    ) { model, overlayNavigator ->
      Impression {
        ScreenViewEvent("Chapters", ScreenType.Overlay)
      }

      AudioTrackListBottomSheet(
        audioTracks = model.audioTracks,
        currentAudioTrack = model.currentAudioTrack,
        playbackSpeed = playbackSpeed,
        onAudioTrackClick = { track ->
          overlayNavigator.finish(AudioTrackResult.Selected(track))
        },
        modifier = Modifier.navigationBarsPadding(),
      )
    },
  )
}

@Composable
private fun AudioTrackListBottomSheet(
  audioTracks: List<AudioTrack>,
  currentAudioTrack: AudioTrack?,
  playbackSpeed: Float,
  onAudioTrackClick: (AudioTrack) -> Unit,
  modifier: Modifier = Modifier,
  progressColor: Color = MaterialTheme.colorScheme.primaryContainer,
  selectedColor: Color = MaterialTheme.colorScheme.primary,
  component: AudioTrackListBottomSheetComponent = rememberComponent(),
) {
  val showTimeInBook by remember {
    component.settings.observeShowTimeInBook()
  }.collectAsState(true)

  val sessionSheetState = rememberSessionSheetTitleState(
    // It is likely that this list will be pre-scrolled when the sheet is open
    // so we need to let the state know to keep
    isAlreadyScrolled = audioTracks
      .indexOfFirst { it.index == currentAudioTrack?.index }
      .takeIf { it != -1 } != 0,
  )

  SessionSheetLayout(
    modifier = modifier,
    state = sessionSheetState,
    title = {
      Text(
        stringResource(Res.string.audio_tracks_bottomsheet_title),
      )
    },
    trailingContent = {
      Switch(
        checked = showTimeInBook,
        onCheckedChange = { component.settings.showTimeInBook = it },
        thumbContent = {
          Icon(
            if (showTimeInBook) CampfireIcons.Rounded.BookRibbon else Icons.Rounded.Timer,
            contentDescription = null,
            modifier = Modifier.size(SwitchDefaults.IconSize),
          )
        },
        modifier = Modifier
          .align(Alignment.CenterEnd),
      )
    },
  ) {
    val lazyListState = rememberLazyListState(
      initialFirstVisibleItemIndex = audioTracks
        .indexOfFirst { it.index == currentAudioTrack?.index }
        .takeIf { it != -1 } ?: 0,
    )

    val isScrolled by remember {
      derivedStateOf {
        lazyListState.firstVisibleItemIndex > 0
      }
    }

    LaunchedEffect(isScrolled) {
      sessionSheetState.isScrolled = isScrolled
    }

    LazyColumn(
      state = lazyListState,
    ) {
      items(
        items = audioTracks,
        key = { it.index },
      ) { track ->
        val isCurrentChapter = currentAudioTrack?.index == track.index

        ListItem(
          headlineContent = {
            Text(
              text = track.taggedTitle,
              fontWeight = if (isCurrentChapter) FontWeight.Bold else null,
            )
          },
          trailingContent = {
            val isAccelerated = playbackSpeed != 1f
            val displayDuration = if (showTimeInBook) {
              track.startOffset.seconds
            } else {
              track.duration.seconds
            }.div(playbackSpeed.toDouble())
            Text(
              text = displayDuration.clockFormat(),
              style = MaterialTheme.typography.labelLarge,
              fontWeight = if (isCurrentChapter) FontWeight.Bold else FontWeight.SemiBold,
              fontFamily = FontFamily.Monospace,
              fontStyle = if (isAccelerated) FontStyle.Italic else null,
              color = if (isAccelerated) MaterialTheme.colorScheme.secondary else Color.Unspecified,
            )
          },
          modifier = Modifier
            .clickable {
              onAudioTrackClick(track)
            }
            .fluentIf(isCurrentChapter) {
              drawBehind {
                val width = size.width // * progress
                drawRect(
                  color = progressColor,
                  topLeft = Offset(-ProgressCornerRadius.toPx(), 0f),
                  size = size.copy(width = width + ProgressCornerRadius.toPx()),
                )

                drawRoundRect(
                  color = selectedColor,
                  topLeft = Offset(-IndicatorSize.toPx(), IndicatorPadding.toPx()),
                  size = Size(IndicatorSize.toPx() * 2f, size.height - (IndicatorPadding * 2).toPx()),
                  cornerRadius = CornerRadius(IndicatorSize.toPx()),
                )
              }
            },
          colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
          ),
        )
      }
    }
  }
}

private val IndicatorSize = 8.dp
private val IndicatorPadding = 0.dp
private val ProgressCornerRadius = 24.dp
