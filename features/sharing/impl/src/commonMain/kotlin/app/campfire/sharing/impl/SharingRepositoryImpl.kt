package app.campfire.sharing.impl

import app.campfire.CampfireDatabase
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.MediaShare
import app.campfire.core.model.MediaShareId
import app.campfire.core.session.UserSession
import app.campfire.core.session.requiredUserId
import app.campfire.data.mapping.asDbModel
import app.campfire.data.mapping.asDomainModel
import app.campfire.data.mapping.asNetworkModel
import app.campfire.network.AudioBookShelfApi
import app.campfire.sharing.SharingRepository
import app.campfire.user.api.UserRepository
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@ContributesBinding(UserScope::class)
@Inject
class SharingRepositoryImpl(
  private val userSession: UserSession,
  private val api: AudioBookShelfApi,
  private val db: CampfireDatabase,
  private val userRepository: UserRepository,
  private val dispatcherProvider: DispatcherProvider,
) : SharingRepository {

  override fun observeAllShares(): Flow<List<MediaShare>> {
    return userRepository.observeCurrentUser()
      .flatMapLatest { user ->
        db.mediaShareQueries
          .selectAll(user.id)
          .asFlow()
          .mapToList(dispatcherProvider.databaseRead)
          .map { dbItems ->
            dbItems.map { it.asDomainModel() }
          }
      }
  }

  override suspend fun createMediaShare(
    slug: String,
    libraryItem: LibraryItem,
    expiresAtEpochMs: Long,
    isDownloadable: Boolean,
  ): Result<MediaShare> = withContext(dispatcherProvider.io) {
    api.shareItem(
      slug = slug,
      mediaItemType = libraryItem.mediaType.asNetworkModel(),
      mediaItemId = libraryItem.id,
      expiresAtEpochMs = expiresAtEpochMs,
      isDownloadable = isDownloadable,
    ).map { response ->
      // Store into database
      val dbModel = response.asDbModel(userSession.requiredUserId)
      withContext(dispatcherProvider.databaseWrite) {
        db.mediaShareQueries
          .insert(dbModel)
      }

      // Return model
      response.asDomainModel()
    }
  }

  override suspend fun deleteShare(
    shareId: MediaShareId,
  ): Result<Unit> = withContext(dispatcherProvider.io) {
    api.deleteShareItem(shareId).onSuccess {
      withContext(dispatcherProvider.databaseWrite) {
        db.mediaShareQueries.delete(shareId)
      }
    }
  }
}
