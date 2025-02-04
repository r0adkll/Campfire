package app.campfire.core.model

import kotlin.time.Duration
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

data class ListeningStats(
  val totalTime: Duration,
  val today: Duration,
  val days: Map<LocalDate, Duration>,
  val dayOfWeek: Map<DayOfWeek, Duration>,
  val items: List<ItemListenedTo>,
  val recentSessions: List<PlaybackSession>,
)

data class ItemListenedTo(
  val id: String,
  val timeListening: Duration,
  val coverImageUrl: String,
  val mediaMetadata: Media.Metadata,
)
