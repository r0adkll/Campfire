package app.campfire.analytics.events

import kotlin.jvm.JvmInline

@Suppress("FunctionName")
fun PlaybackActionEvent(
  obj: PlaybackObj,
  verb: Verb,
  noun: PlaybackNoun? = null,
  extras: Map<String, Any>? = null,
) = ActionEvent(
  obj = obj.value,
  verb = verb,
  noun = noun?.value,
  extras = extras,
)

// Objects
@JvmInline
value class PlaybackObj internal constructor(val value: String)

val PlayPause get() = PlaybackObj("play_pause")
val Rewind get() = PlaybackObj("rewind")
val Forward get() = PlaybackObj("forward")
val SkipPrevious get() = PlaybackObj("skip_previous")
val SkipNext get() = PlaybackObj("skip_next")
val Seek get() = PlaybackObj("seek")
val Timer get() = PlaybackObj("sleep_timer")
val Chapter get() = PlaybackObj("chapter")
val AudioTrack get() = PlaybackObj("track")
val Bookmark get() = PlaybackObj("bookmark")
val Speed get() = PlaybackObj("playback_speed")

// Verbs
val Changed get() = Verb("changed")
val Cleared get() = Verb("cleared")

// Nouns
@JvmInline
value class PlaybackNoun internal constructor(val value: String)

val PlaybackBar get() = PlaybackNoun("playback_bar")
