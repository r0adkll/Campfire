package app.campfire.home.api

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

/**
 * TODO: Roll this into our existing [app.campfire.core.coroutines.LoadState] construct by adding
 *  the different Error states there.
 */
sealed interface FeedResponse<DataT> {

  val dataOrNull: DataT?
    get() = (this as? Success<DataT>)?.data

  data object Loading : FeedResponse<Nothing>
  data class Success<DataT>(val data: DataT) : FeedResponse<DataT>
  sealed interface Error : FeedResponse<Nothing> {
    data class Exception(val error: Throwable) : Error
    data class Message(val message: String) : Error
  }
}

@Suppress("UNCHECKED_CAST")
@OptIn(ExperimentalCoroutinesApi::class)
inline fun <Data, Result> Flow<FeedResponse<Data>>.flatMapLatestSuccess(
  crossinline mapper: suspend (Data) -> Flow<Result>,
): Flow<FeedResponse<Result>> {
  return flatMapLatest { response ->
    when (response) {
      is FeedResponse.Error -> this as Flow<FeedResponse<Result>>
      FeedResponse.Loading -> this as Flow<FeedResponse<Result>>
      is FeedResponse.Success<Data> -> {
        mapper(response.data).map {
          FeedResponse.Success(it)
        }
      }
    }
  }
}
