package app.campfire.libraries

import app.campfire.CampfireDatabase
import app.campfire.common.settings.CampfireSettings
import app.campfire.core.di.AppScope
import app.campfire.core.model.Library
import app.campfire.libraries.api.LibraryRepository
import app.campfire.network.AudioBookShelfApi
import com.r0adkll.kotlininject.merge.annotations.ContributesBinding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import me.tatarka.inject.annotations.Inject
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.FetcherResult
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.StoreBuilder

@ContributesBinding(AppScope::class)
@Inject
class StoreLibraryRepository(
  private val api: AudioBookShelfApi,
  private val db: CampfireDatabase,
  private val settings: CampfireSettings,
) : LibraryRepository {

  private val libraryStore = StoreBuilder.from(
    fetcher = Fetcher.ofResult { api.getAllLibraries().asFetcherResult() },
    sourceOfTruth = SourceOfTruth.of(
      reader = { emptyFlow() },
      writer = { key, data -> },
      delete = { },
      deleteAll = { },
    )
  ).build()

  override fun observeAllLibraries(): Flow<List<Library>> {
    TODO("Not yet implemented")
  }
}

fun <T : Any> Result<T>.asFetcherResult(): FetcherResult<T> {
  return when {
    isSuccess -> FetcherResult.Data(getOrThrow(), "api")
    else -> FetcherResult.Error.Exception(exceptionOrNull()!!)
  }
}
