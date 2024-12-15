package app.campfire.sessions.network

import app.campfire.core.model.Session
import app.campfire.network.models.PlaybackSession

interface NetworkSessionMapper {
  suspend fun map(session: Session): PlaybackSession
}
