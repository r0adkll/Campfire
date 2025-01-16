package app.campfire.audioplayer.impl.player

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import platform.MediaPlayer.MPChangePlaybackRateCommand
import platform.MediaPlayer.MPRemoteCommand
import platform.MediaPlayer.MPRemoteCommandEvent
import platform.MediaPlayer.MPRemoteCommandHandlerStatus
import platform.MediaPlayer.MPRemoteCommandHandlerStatusCommandFailed
import platform.MediaPlayer.MPSkipIntervalCommand

fun <Command : MPRemoteCommand> Command.enable(
  enabled: Boolean = true,
  setup: Command.() -> Unit = {},
  action: (MPRemoteCommandEvent) -> MPRemoteCommandHandlerStatus,
) {
  setEnabled(enabled)
  removeTarget(null)
  setup()
  addTargetWithHandler { event ->
    if (event == null) return@addTargetWithHandler MPRemoteCommandHandlerStatusCommandFailed
    action(event)
  }
}

fun MPSkipIntervalCommand.setPreferredIntervals(vararg intervalsInMillis: Long) {
  preferredIntervals = intervalsInMillis.map { it.milliseconds.toDouble(DurationUnit.SECONDS) }
}

fun MPSkipIntervalCommand.getPreferredIntervals(): List<Duration> {
  return preferredIntervals
    .filterIsInstance<Double>()
    .map { it.seconds }
}

fun MPChangePlaybackRateCommand.setSupportedPlaybackRates(rates: List<Float>) {
  supportedPlaybackRates = rates.map { it.toDouble() }
}
