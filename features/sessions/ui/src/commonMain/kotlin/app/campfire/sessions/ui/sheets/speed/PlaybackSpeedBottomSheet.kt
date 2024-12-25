package app.campfire.sessions.ui.sheets.speed

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import app.campfire.audioplayer.AudioPlayerHolder
import app.campfire.core.di.AppScope
import app.campfire.core.di.ComponentHolder
import app.campfire.core.extensions.toString
import app.campfire.sessions.ui.sheets.SessionSheetLayout
import campfire.features.sessions.ui.generated.resources.Res
import campfire.features.sessions.ui.generated.resources.speed_bottomsheet_title
import com.r0adkll.kimchi.annotations.ContributesTo
import com.slack.circuit.overlay.OverlayHost
import com.slack.circuitx.overlays.BottomSheetOverlay
import ir.mahozad.multiplatform.wavyslider.WaveDirection
import ir.mahozad.multiplatform.wavyslider.material3.WavySlider
import kotlin.math.roundToInt
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import org.jetbrains.compose.resources.stringResource

suspend fun OverlayHost.showPlaybackSpeedBottomSheet(speed: Float) {
  show(
    BottomSheetOverlay(
      model = speed,
      onDismiss = { },
      sheetShape = RoundedCornerShape(
        topStart = 32.dp,
        topEnd = 32.dp,
      ),
      skipPartiallyExpandedState = true,
    ) { s, _ ->
      PlaybackSpeedBottomSheet(
        speed = s,
        modifier = Modifier.navigationBarsPadding(),
      )
    },
  )
}

@ContributesTo(AppScope::class)
interface PlaybackSpeedBottomSheetComponent {
  val audioPlayerHolder: AudioPlayerHolder
}

@Composable
private fun rememberAudioPlayerHolder(): AudioPlayerHolder {
  return remember {
    ComponentHolder.component<PlaybackSpeedBottomSheetComponent>()
      .audioPlayerHolder
  }
}

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
private fun PlaybackSpeedBottomSheet(
  speed: Float,
  modifier: Modifier = Modifier,
  audioPlayerHolder: AudioPlayerHolder = rememberAudioPlayerHolder(),
) {
  val currentSpeed by remember {
    audioPlayerHolder.currentPlayer
      .flatMapLatest {
        it?.playbackSpeed ?: emptyFlow()
      }
  }.collectAsState(speed)

  SessionSheetLayout(
    modifier = modifier,
    title = { Text(stringResource(Res.string.speed_bottomsheet_title)) },
  ) {
    Spacer(Modifier.height(16.dp))

    SingleChoiceSegmentedButtonRow(
      modifier = Modifier.align(Alignment.CenterHorizontally),
    ) {
      DefaultSpeedOptions.forEachIndexed { index, defaultSpeed ->
        val isCurrentSpeed = currentSpeed == defaultSpeed
        SegmentedButton(
          shape = SegmentedButtonDefaults.itemShape(index, DefaultSpeedOptions.size, RoundedCornerShape(16.dp)),
          selected = isCurrentSpeed,
          label = { Text("${defaultSpeed.readable}x") },
          onClick = {
            audioPlayerHolder.currentPlayer.value
              ?.setPlaybackSpeed(defaultSpeed)
          },
        )
      }
    }

    Spacer(Modifier.height(16.dp))

    Row(
      modifier = Modifier
        .height(56.dp)
        .fillMaxSize()
        .padding(horizontal = 20.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      var sliderValue by remember { mutableStateOf(speed) }

      LaunchedEffect(currentSpeed) {
        if (sliderValue != currentSpeed) {
          sliderValue = currentSpeed
        }
      }

      val sliderProgressNormalized = sliderValue / (DefaultSpeedOptions.min() + DefaultSpeedOptions.length)
      val waveLength = lerp(WavelengthRange.endInclusive, WavelengthRange.start, sliderProgressNormalized)
      val waveHeight = lerp(WaveHeightRange.start, WaveHeightRange.endInclusive, sliderProgressNormalized)
      val waveVelocity = lerp(WaveVelocityRange.start, WaveVelocityRange.endInclusive, sliderProgressNormalized)
      val waveThickness = lerp(WaveThicknessRange.start, WaveThicknessRange.endInclusive, sliderProgressNormalized)

      WavySlider(
        value = sliderValue,
        valueRange = DefaultSpeedOptions.asRange,
        onValueChange = {
          sliderValue = it
          audioPlayerHolder.currentPlayer.value?.setPlaybackSpeed(it)
        },
        waveLength = waveLength,
        waveHeight = waveHeight,
        waveVelocity = waveVelocity to WaveDirection.TAIL,
        waveThickness = waveThickness,
        incremental = true,
        modifier = Modifier.weight(1f),
      )

      Text(
        text = "${sliderValue.readable}x",
        textAlign = TextAlign.Right,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier
          .padding(horizontal = 24.dp)
          .width(32.dp),
      )
    }

    Spacer(Modifier.height(24.dp))
  }
}

private val Float.readable: String
  get() {
    val asInt = roundToInt()
    val isWhole = this == asInt.toFloat()
    return if (isWhole) {
      "$asInt"
    } else {
      toString(1)
    }
  }

private val DefaultSpeedOptions = listOf(3f, 2f, 1.5f, 1f, 0.5f)

private val WavelengthRange = 40.dp.rangeTo(135.dp)
private val WaveHeightRange = 0.dp.rangeTo(40.dp)
private val WaveVelocityRange = 50.dp.rangeTo(120.dp)
private val WaveThicknessRange = 16.dp.rangeTo(6.dp)

private val List<Float>.asRange: ClosedFloatingPointRange<Float>
  get() = min().rangeTo(max())

private val List<Float>.length: Float get() = max() - min()
