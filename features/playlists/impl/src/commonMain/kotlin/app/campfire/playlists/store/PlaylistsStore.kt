// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.playlists.store

import app.campfire.CampfireDatabase
import app.campfire.account.api.UrlHydrator
import app.campfire.account.api.UserSessionManager
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.logging.Cork
import app.campfire.core.model.CollectionId
import app.campfire.core.model.LibraryId
import app.campfire.core.model.Playlist
import app.campfire.core.model.PlaylistId
import app.campfire.core.model.UserId
import app.campfire.core.time.FatherTime
import app.campfire.data.mapping.dao.LibraryItemDao
import app.campfire.network.AudioBookShelfApi
import kotlin.uuid.Uuid
import me.tatarka.inject.annotations.Inject
import org.mobilenativefoundation.store.store5.Converter
import org.mobilenativefoundation.store.store5.ExperimentalStoreApi
import org.mobilenativefoundation.store.store5.MutableStore
import org.mobilenativefoundation.store.store5.MutableStoreBuilder

object PlaylistsStore : Cork {

  override val tag: String = "PlaylistsStore"
  override val enabled: Boolean = false

  @Inject
  class Factory(
    api: AudioBookShelfApi,
    db: CampfireDatabase,
    libraryItemDao: LibraryItemDao,
    urlHydrator: UrlHydrator,
    fatherTime: FatherTime,
    userSessionManager: UserSessionManager,
    dispatcherProvider: DispatcherProvider,
  ) {
    private val fetcherFactory = PlaylistsFetcherFactory(api, urlHydrator)
    internal val sourceOfTruthFactory =
      PlaylistsSourceOfTruthFactory(
        db = db,
        libraryItemDao = libraryItemDao,
        dispatcherProvider = dispatcherProvider,
        urlHydrator = urlHydrator,
        fatherTime = fatherTime,
      )
    private val updaterFactory = PlaylistsUpdaterFactory(api, db, dispatcherProvider)
    private val bookkeeperFactory = PlaylistsBookKeeperFactory(userSessionManager, db, dispatcherProvider)

    @OptIn(ExperimentalStoreApi::class)
    fun create(): MutableStore<Operation, Output> {
      return MutableStoreBuilder.from(
        fetcher = fetcherFactory.create(),
        sourceOfTruth = sourceOfTruthFactory.create(),
        converter = createConverter(),
      ).build(
        updater = updaterFactory.create(),
        bookkeeper = bookkeeperFactory.create(),
      )
    }

    private fun createConverter(): Converter<List<Playlist>, List<Playlist>, Output> {
      return Converter.Builder<List<Playlist>, List<Playlist>, Output>()
        // We pre-convert these in the fetcher so we can perform the token hydration
        // in a coroutine rather than using runBlocking { } here.
        .fromNetworkToLocal { network -> network }
        .fromOutputToLocal { output ->
          when (output) {
            is Output.Single -> listOf(output.playlist)
            is Output.Collection -> output.playlists
          }
        }
        .build()
    }
  }

  sealed interface Operation {
    data class All(
      val userId: UserId,
      val libraryId: LibraryId,
    ) : Operation

    data class Single(
      val userId: UserId,
      val libraryId: LibraryId,
      val playlistId: PlaylistId,
      val isCreatedId: Boolean = false,
    ) : Operation

    sealed class Mutation(
      val key: String,
    ) : Operation {
      abstract val userId: UserId

      data class Create(
        override val userId: UserId,
        val name: String,
        val description: String?,
        val libraryId: LibraryId,
        val items: List<Playlist.Item.Minified>,
        val creationId: String = Uuid.random().toHexDashString(),
      ) : Mutation("create")

      data class Update(
        override val userId: UserId,
        val id: PlaylistId,
        val name: String,
        val description: String?,
        val items: List<Playlist.Item.Minified>,
      ) : Mutation("update")

      data class Delete(
        override val userId: UserId,
        val id: PlaylistId,
      ) : Mutation("delete")

      data class Add(
        override val userId: UserId,
        val playlistId: PlaylistId,
        val item: Playlist.Item.Minified,
      ) : Mutation("add")

      data class Remove(
        override val userId: UserId,
        val playlistId: PlaylistId,
        val item: Playlist.Item.Minified,
      ) : Mutation("remove")

      data class FromCollection(
        override val userId: UserId,
        val collectionId: CollectionId,
        val libraryId: LibraryId,
      ) : Mutation("fromCollection")
    }
  }

  sealed interface Output {
    fun isEmpty(): Boolean

    data class Single(val playlist: Playlist) : Output {
      override fun isEmpty(): Boolean = false
    }

    data class Collection(val playlists: List<Playlist>) : Output {
      override fun isEmpty(): Boolean = playlists.isEmpty()
    }
  }

  sealed interface Update {
    data object Success : Update
    data object Failure : Update
  }
}
