package app.campfire.libraries.socket

import app.campfire.core.model.LibraryId
import app.campfire.core.model.LibraryItemId
import app.campfire.network.models.Folder
import app.campfire.network.models.Library
import app.campfire.network.models.LibraryItemExpanded
import app.campfire.network.models.LibrarySettings
import app.campfire.network.models.MediaType
import app.campfire.socket.events.ItemRemoved
import app.campfire.socket.events.LibraryAdded
import app.campfire.socket.events.LibraryRemoved
import app.campfire.socket.events.LibraryUpdated
import app.campfire.socket.events.UserSessionClosed
import app.campfire.socket.payloads.ItemRemovedPayload
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class LibraryCacheInvalidationListenerTest {

  private val itemHandler = RecordingLibraryItemEventHandler()
  private val libraryHandler = RecordingLibraryEventHandler()
  private val listener = LibraryCacheInvalidationListener(itemHandler, libraryHandler)

  @Test
  fun `item removed routes to item handler with the id`() = runTest {
    listener.handle(ItemRemoved(ItemRemovedPayload(id = "li-1", libraryId = "lib-1")))

    assertThat(itemHandler.invocations).containsExactly(ItemCall.Removed("li-1"))
    assertThat(libraryHandler.invocations).isEmpty()
  }

  @Test
  fun `library added routes to library handler with the library`() = runTest {
    val library = sampleLibrary(id = "lib-1")
    listener.handle(LibraryAdded(library))

    assertThat(libraryHandler.invocations).containsExactly(LibraryCall.Added(library))
    assertThat(itemHandler.invocations).isEmpty()
  }

  @Test
  fun `library updated routes to library handler`() = runTest {
    val library = sampleLibrary(id = "lib-2")
    listener.handle(LibraryUpdated(library))

    assertThat(libraryHandler.invocations).containsExactly(LibraryCall.Updated(library))
  }

  @Test
  fun `library removed routes to library handler with the id`() = runTest {
    val library = sampleLibrary(id = "lib-3")
    listener.handle(LibraryRemoved(library))

    assertThat(libraryHandler.invocations).containsExactly(LibraryCall.Removed("lib-3"))
  }

  @Test
  fun `unrelated events are ignored`() = runTest {
    listener.handle(UserSessionClosed(sessionId = "session-1"))

    assertThat(itemHandler.invocations).isEmpty()
    assertThat(libraryHandler.invocations).isEmpty()
  }

  private fun sampleLibrary(id: String) = Library(
    id = id,
    name = "Test Library $id",
    folders = emptyList<Folder>(),
    displayOrder = 1,
    icon = "audiobook",
    mediaType = MediaType.Book,
    provider = "audible",
    settings = LibrarySettings(coverAspectRatio = 1, disableWatcher = false),
    createdAt = 0L,
    lastUpdate = 0L,
  )

  private sealed interface ItemCall {
    data class Added(val item: LibraryItemExpanded) : ItemCall
    data class Updated(val item: LibraryItemExpanded) : ItemCall
    data class Removed(val itemId: LibraryItemId) : ItemCall
    data class BatchAdded(val items: List<LibraryItemExpanded>) : ItemCall
    data class BatchUpdated(val items: List<LibraryItemExpanded>) : ItemCall
  }

  private class RecordingLibraryItemEventHandler : LibraryItemEventHandler {
    val invocations = mutableListOf<ItemCall>()
    override suspend fun onItemAdded(item: LibraryItemExpanded) {
      invocations += ItemCall.Added(item)
    }
    override suspend fun onItemUpdated(item: LibraryItemExpanded) {
      invocations += ItemCall.Updated(item)
    }
    override suspend fun onItemRemoved(itemId: LibraryItemId) {
      invocations += ItemCall.Removed(itemId)
    }
    override suspend fun onItemsAdded(items: List<LibraryItemExpanded>) {
      invocations += ItemCall.BatchAdded(items)
    }
    override suspend fun onItemsUpdated(items: List<LibraryItemExpanded>) {
      invocations += ItemCall.BatchUpdated(items)
    }
  }

  private sealed interface LibraryCall {
    data class Added(val library: Library) : LibraryCall
    data class Updated(val library: Library) : LibraryCall
    data class Removed(val libraryId: LibraryId) : LibraryCall
  }

  private class RecordingLibraryEventHandler : LibraryEventHandler {
    val invocations = mutableListOf<LibraryCall>()
    override suspend fun onLibraryAdded(library: Library) {
      invocations += LibraryCall.Added(library)
    }
    override suspend fun onLibraryUpdated(library: Library) {
      invocations += LibraryCall.Updated(library)
    }
    override suspend fun onLibraryRemoved(libraryId: LibraryId) {
      invocations += LibraryCall.Removed(libraryId)
    }
  }
}
