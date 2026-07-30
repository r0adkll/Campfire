// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.user.mediaprogress.store

import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PodcastEpisodeId
import app.campfire.data.mapping.asDomainModel
import app.campfire.data.mapping.asFetcherResult
import app.campfire.network.AudioBookShelfApi
import app.campfire.user.mediaprogress.store.MediaProgressStore.Operation
import app.campfire.user.mediaprogress.store.MediaProgressStore.Output
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.FetcherResult

class MediaProgressFetcherFactory(
  private val api: AudioBookShelfApi,
) {

  fun create(): Fetcher<Operation, Output> {
    return Fetcher.ofResult { operation ->
      require(operation is Operation.Query)
      when (operation) {
        is Operation.Query.All -> fetchAll()
        is Operation.Query.One -> fetchSingle(operation.libraryItemId, operation.episodeId)
      }
    }
  }

  private suspend fun fetchAll(): FetcherResult<Output.Collection> {
    return api.getCurrentUser()
      .map { Output.Collection(it.mediaProgress.map { p -> p.asDomainModel() }) }
      .asFetcherResult()
  }

  private suspend fun fetchSingle(
    libraryItemId: LibraryItemId,
    episodeId: PodcastEpisodeId?,
  ): FetcherResult<Output.Single> {
    return api.getMediaProgress(libraryItemId, episodeId)
      .map { Output.Single(it.asDomainModel()) }
      .asFetcherResult()
  }
}
