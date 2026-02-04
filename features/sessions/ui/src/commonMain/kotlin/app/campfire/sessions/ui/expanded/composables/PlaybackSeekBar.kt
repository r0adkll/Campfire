package app.campfire.sessions.ui.expanded.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.campfire.audioplayer.AudioPlayer
import app.campfire.common.compose.extensions.readoutFormat
import app.campfire.core.extensions.fluentIf
import ir.mahozad.multiplatform.wavyslider.WaveDirection
import ir.mahozad.multiplatform.wavyslider.material3.WavySlider
import kotlin.time.Duration

@Composable
internal fun PlaybackSeekBar(
  state: AudioPlayer.State,
  currentTime: Duration,
  currentDuration: Duration,
  playbackSpeed: Float,
  onSeek: (Float) -> Unit,
  modifier: Modifier = Modifier,
  interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
  Column(
    modifier = modifier,
  ) {
    val isPressed by interactionSource.collectIsPressedAsState()
    val isDragged by interactionSource.collectIsDraggedAsState()
    val isInteracting = isPressed || isDragged

    fun calculateProgress(): Float = if (currentDuration.inWholeMilliseconds == 0L) {
      0f
    } else {
      (currentTime / currentDuration).toFloat()
    }

    var sliderValue by remember { mutableStateOf(calculateProgress()) }
    val softSliderValue by animateFloatAsState(sliderValue)
    LaunchedEffect(isInteracting, state, currentTime, currentDuration) {
      if (!isInteracting) {
        sliderValue = calculateProgress()
      }
    }

    val waveHeight = if (state == AudioPlayer.State.Playing) 16.dp else 0.dp
    val waveVelocity = if (state == AudioPlayer.State.Playing) 40.dp else 0.dp
    val waveThickness = if (state == AudioPlayer.State.Playing) 12.dp else 16.dp

    WavySlider(
      enabled = state == AudioPlayer.State.Playing || state == AudioPlayer.State.Paused,
      value = softSliderValue,
      onValueChange = { sliderValue = it },
      onValueChangeFinished = {
        onSeek(sliderValue)
      },
      interactionSource = interactionSource,
      colors = SliderDefaults.colors(
        inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainer,
      ),
      waveLength = 50.dp,
      waveHeight = waveHeight,
      waveVelocity = waveVelocity to WaveDirection.TAIL,
      waveThickness = waveThickness,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp),
    )

    Row(
      Modifier
        .fillMaxWidth()
        .padding(horizontal = 32.dp),
    ) {
      val currentTimeLabel = if (isInteracting) {
        currentDuration.times(sliderValue.toDouble()).readoutFormat()
      } else {
        currentTime.readoutFormat()
      }

      Text(
        text = currentTimeLabel,
        style = MaterialTheme.typography.labelSmall,
      )

      Spacer(Modifier.weight(1f))

      val isAccelerated = playbackSpeed != 1f
      AnimatedVisibility(
        visible = isAccelerated,
      ) {
        Icon(
          Icons.Rounded.KeyboardDoubleArrowRight,
          contentDescription = null,
          modifier = Modifier.size(16.dp),
          tint = MaterialTheme.colorScheme.secondary,
        )
      }

      val currentRemainingDuration = (currentDuration - currentTime).div(playbackSpeed.toDouble())
      Text(
        text = currentRemainingDuration.readoutFormat(),
        style = MaterialTheme.typography.labelSmall.fluentIf(isAccelerated) {
          copy(
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.secondary,
            fontSize = 12.sp,
          )
        },
      )
    }
  }
}
