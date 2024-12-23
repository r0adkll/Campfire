package app.campfire.sessions.db

import app.campfire.core.model.LibraryItemId

interface MediaProgressDataSource {

  suspend fun deleteMediaProgress(libraryItemId: LibraryItemId)
}
