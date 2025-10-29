package app.campfire.home.api

import app.campfire.home.api.model.Shelf
import kotlinx.coroutines.flow.Flow

interface HomeRepository {

  /**
   * Observe a flow of the users personalized home feed
   */
  fun observeHomeFeed(): Flow<HomeFeedResponse>
}

sealed interface HomeFeedResponse {

  val dataOrNull: List<Shelf<*>>?
    get() = (this as? Success)?.data

  data object Loading : HomeFeedResponse
  data class Success(val data: List<Shelf<*>>) : HomeFeedResponse
  sealed interface Error : HomeFeedResponse {
    data class Exception(val error: Throwable) : Error
    data class Message(val message: String) : Error
  }
}
