package app.campfire.home.progress

import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.MediaProgress

interface MediaProgressDataSource {

  suspend fun getMediaProgress(libraryItemId: LibraryItemId): MediaProgress?
}
