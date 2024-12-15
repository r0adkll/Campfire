package app.campfire.sessions.api

import app.campfire.core.model.LibraryItemId

interface SessionSynchronizer {

  suspend fun sync(libraryItemId: LibraryItemId)
}
