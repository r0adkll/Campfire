package app.campfire.network.models

import kotlinx.serialization.Serializable

@Serializable
data class ListeningStats(
  val totalTime: Float,
  val today: Float,
  val days: Map<String, Float>,
  val dayOfWeek: Map<String, Float>,
  val items: Map<String, ItemsListenedTo>,
  val recentSessions: List<PlaybackSession>,
)

@Serializable
data class ItemsListenedTo(
  val id: String,
  val timeListening: Float,
  val mediaMetadata: ExpandedBookMetadata,
)
