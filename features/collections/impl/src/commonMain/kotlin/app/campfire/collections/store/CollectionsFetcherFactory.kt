package app.campfire.collections.store

import app.campfire.account.api.UrlHydrator
import app.campfire.core.model.Collection
import app.campfire.data.mapping.asDomainModel
import app.campfire.data.mapping.asFetcherResult
import app.campfire.network.AudioBookShelfApi
import org.mobilenativefoundation.store.store5.Fetcher

class CollectionsFetcherFactory(
  private val api: AudioBookShelfApi,
  private val urlHydrator: UrlHydrator,
) {

  fun create(): Fetcher<CollectionsStore.Operation, List<Collection>> {
    return Fetcher.ofResult { operation ->
      require(operation is CollectionsStore.Operation.All || operation is CollectionsStore.Operation.Single)
      when (operation) {
        is CollectionsStore.Operation.All -> {
          api.getCollections(operation.libraryId)
            .map { collections ->
              collections.map { collection ->
                collection.asDomainModel(urlHydrator)
              }
            }
            .asFetcherResult()
        }
        is CollectionsStore.Operation.Single -> api.getCollection(operation.collectionId)
          .map { listOf(it.asDomainModel(urlHydrator)) }
          .asFetcherResult()
        else -> throw IllegalArgumentException("Unknown operation: $operation")
      }
    }
  }
}
