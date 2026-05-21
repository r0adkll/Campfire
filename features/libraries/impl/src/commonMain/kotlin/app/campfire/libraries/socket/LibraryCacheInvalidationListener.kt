package app.campfire.libraries.socket

import app.campfire.core.di.UserScope
import app.campfire.socket.events.ItemAdded
import app.campfire.socket.events.ItemRemoved
import app.campfire.socket.events.ItemUpdated
import app.campfire.socket.events.ItemsAdded
import app.campfire.socket.events.ItemsUpdated
import app.campfire.socket.events.LibraryAdded
import app.campfire.socket.events.LibraryRemoved
import app.campfire.socket.events.LibraryUpdated
import app.campfire.socket.events.SocketEvent
import app.campfire.socket.events.SocketEventListener
import com.r0adkll.kimchi.annotations.ContributesMultibinding
import me.tatarka.inject.annotations.Inject

@ContributesMultibinding(UserScope::class, boundType = SocketEventListener::class)
@Inject
class LibraryCacheInvalidationListener(
  private val itemHandler: LibraryItemEventHandler,
  private val libraryHandler: LibraryEventHandler,
) : SocketEventListener {
  override suspend fun handle(event: SocketEvent) {
    when (event) {
      is ItemAdded -> itemHandler.onItemAdded(event.item)
      is ItemUpdated -> itemHandler.onItemUpdated(event.item)
      is ItemRemoved -> itemHandler.onItemRemoved(event.payload.id)
      is ItemsAdded -> itemHandler.onItemsAdded(event.items)
      is ItemsUpdated -> itemHandler.onItemsUpdated(event.items)
      is LibraryAdded -> libraryHandler.onLibraryAdded(event.library)
      is LibraryUpdated -> libraryHandler.onLibraryUpdated(event.library)
      is LibraryRemoved -> libraryHandler.onLibraryRemoved(event.library.id)
      else -> Unit
    }
  }
}
