package app.campfire.network.envelopes

import kotlinx.serialization.Serializable

@Serializable
data class SyncLocalSessionsResult(
  val results: List<SessionSyncResult>,
) {
  private val mappedResults: Map<String, SessionSyncResult> = results.associateBy { it.id }

  operator fun get(id: String): SessionSyncResult? = mappedResults[id]
}

@Serializable
data class SessionSyncResult(
  val id: String,
  val success: Boolean,
  val error: String? = null,
  val progressSynced: Boolean = false,
)
