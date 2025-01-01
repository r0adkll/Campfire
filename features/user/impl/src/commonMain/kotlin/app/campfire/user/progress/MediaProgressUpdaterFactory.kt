package app.campfire.user.progress

import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.MediaProgress
import app.campfire.data.mapping.asNetworkUpdate
import app.campfire.network.AudioBookShelfApi
import app.campfire.user.progress.MediaProgressStore.Operation
import app.campfire.user.progress.MediaProgressStore.Output
import org.mobilenativefoundation.store.store5.Updater
import org.mobilenativefoundation.store.store5.UpdaterResult

// TODO: Should we move this? Its a bit wordy and i'm not sure how its used yet
sealed interface MediaProgressWriteResponse {
  data object Update : MediaProgressWriteResponse
  data object Delete : MediaProgressWriteResponse
}

class MediaProgressUpdaterFactory(
  private val api: AudioBookShelfApi,
) {

  fun create(): Updater<Operation, Output, MediaProgressWriteResponse> = Updater.by(
    post = { operation, output ->
      MediaProgressStore.ibark { "Updater: $operation, output: $output" }
      handleRequest(operation, output)
    }
  )

  private suspend fun handleRequest(operation: Operation, output: Output): UpdaterResult {
    return when (operation) {
      is Operation.Mutation.Update.UpsertOne -> {
        require(output is Output.Single)
        updateOne(output.item)
      }
      is Operation.Mutation.Update.UpsertMany -> {
        require(output is Output.Collection)
        updateMany(output.items)
      }
      is Operation.Mutation.Delete.One -> {
        deleteOne(operation.libraryItemId)
      }

      is Operation.Query.All -> {
        require(output is Output.Collection)
        updateMany(output.items)
      }
      is Operation.Query.One -> {
        require(output is Output.Single)
        updateOne(output.item)
      }
    }
  }

  private suspend fun updateOne(item: MediaProgress): UpdaterResult {
    return api.updateMediaProgress(item.libraryItemId, item.asNetworkUpdate())
      .fold(
        onSuccess = { UpdaterResult.Success.Typed(MediaProgressWriteResponse.Update) },
        onFailure = { UpdaterResult.Error.Exception(it) },
      )
  }

  private suspend fun updateMany(items: List<MediaProgress>): UpdaterResult {
    return api.batchUpdateMediaProgress(items.map { it.asNetworkUpdate(it.libraryItemId) })
      .fold(
        onSuccess = { UpdaterResult.Success.Typed(MediaProgressWriteResponse.Update) },
        onFailure = { UpdaterResult.Error.Exception(it) },
      )
  }

  private suspend fun deleteOne(libraryItemId: LibraryItemId): UpdaterResult {
    return api.deleteMediaProgress(libraryItemId)
      .fold(
        onSuccess = { UpdaterResult.Success.Typed(MediaProgressWriteResponse.Delete) },
        onFailure = { UpdaterResult.Error.Exception(it) },
      )
  }
}
