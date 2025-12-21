package app.campfire.stats

import app.campfire.account.api.UrlHydrator
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryStats
import app.campfire.core.model.ListeningStats
import app.campfire.data.mapping.asDomainModel
import app.campfire.network.AudioBookShelfApi
import app.campfire.stats.api.StatsRepository
import app.campfire.user.api.UserRepository
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import me.tatarka.inject.annotations.Inject

@ContributesBinding(UserScope::class)
@Inject
class NetworkStatsRepository(
  private val api: AudioBookShelfApi,
  private val userRepository: UserRepository,
  private val urlHydrator: UrlHydrator,
) : StatsRepository {

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun getLibraryStats(): Flow<LibraryStats> {
    return userRepository.observeCurrentUser()
      .flatMapLatest { user ->
        flow {
          val result = api.getLibraryStats(user.selectedLibraryId)
          if (result.isSuccess) {
            emit(result.getOrThrow().asDomainModel(urlHydrator))
          } else {
            throw result.exceptionOrNull()!!
          }
        }
      }
  }

  override fun getUserStats(): Flow<ListeningStats> {
    return flow {
      val result = api.getListeningStats()
      if (result.isSuccess) {
        emit(result.getOrThrow().asDomainModel(urlHydrator))
      } else {
        throw result.exceptionOrNull()!!
      }
    }
  }
}
