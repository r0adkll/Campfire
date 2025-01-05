package app.campfire.user.mediaprogress.store

import app.campfire.core.model.LibraryItemId
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
      MediaProgressStore.ibark { "Fetcher: $operation" }
      require(operation is Operation.Query)
      when (operation) {
        is Operation.Query.All -> fetchAll()
        is Operation.Query.One -> fetchSingle(operation.libraryItemId)
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
  ): FetcherResult<Output.Single> {
    return api.getMediaProgress(libraryItemId)
      .map { Output.Single(it.asDomainModel()) }
      .asFetcherResult()
  }
}
