package app.campfire.series

import app.campfire.account.test.FakeUrlHydrator
import app.campfire.common.test.coroutines.asTestDispatcherProvider
import app.campfire.common.test.logging.SystemBark
import app.campfire.common.test.user
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.logging.Heartwood
import app.campfire.core.model.Series
import app.campfire.core.session.UserSession
import app.campfire.db.test.CampfireDatabaseTestInterceptor
import app.campfire.db.test.model.createDbSeries
import app.campfire.home.ui.series
import app.campfire.network.test.FakeAudioBookShelfApi
import app.campfire.network.test.model.createNetworkSeries
import app.campfire.series.store.SeriesStore
import app.campfire.user.test.FakeUserRepository
import app.cash.burst.Burst
import app.cash.burst.InterceptTest
import app.cash.turbine.test
import assertk.all
import assertk.assertThat
import assertk.assertions.index
import assertk.assertions.isEqualTo
import assertk.assertions.prop
import assertk.assertions.single
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import org.mobilenativefoundation.store.cache5.CacheBuilder

@Burst
class StoreSeriesRepositoryTest {

  @InterceptTest
  private val databaseInterceptor = CampfireDatabaseTestInterceptor()

  private val userId = "test_user_id"
  private val libraryId = "test_library_id"
  private val userSession = UserSession.LoggedIn(user(userId))
  private val api = FakeAudioBookShelfApi()
  private val userRepository = FakeUserRepository()
  private val tokenHydrator = FakeUrlHydrator()
  private val cache = CacheBuilder<SeriesStore.Key, List<Series>>().build()

  private val createRepository = { dispatcherProvider: DispatcherProvider ->
    StoreSeriesRepository(
      userSession = userSession,
      api = api,
      db = databaseInterceptor.db,
      userRepository = userRepository,
      urlHydrator = tokenHydrator,
      dispatcherProvider = dispatcherProvider,
      seriesStoreFactory = SeriesStore.Factory(
        userSession = userSession,
        api = api,
        db = databaseInterceptor.db,
        urlHydrator = tokenHydrator,
        dispatcherProvider = dispatcherProvider,
        cache = cache,
      ),
    )
  }

  @BeforeTest
  fun setup() {
    Heartwood.grow(SystemBark)
    userRepository.currentUserFlow.tryEmit(userSession.user)
  }

  @AfterTest
  fun teardown() {
    Heartwood.shrink(SystemBark)
  }

  @Test
  fun observeSeries_Cached() = runTest {
    val repository = createRepository(asTestDispatcherProvider())
    val series = series("test_series_id", "Test Series")
    cache.put(SeriesStore.Key(userId, libraryId), listOf(series))

    repository.observeAllSeries().test {
      assertThat(awaitItem())
        .isEqualTo(listOf(series))
    }
  }

  @Test
  fun observeSeries_CachedWithSoT_EmitsCacheThenSoT() = runTest {
    val repository = createRepository(asTestDispatcherProvider())
    val series = series("test_series_id", "Test Series")

    databaseInterceptor.db
      .seriesQueries
      .insertOrIgnore(createDbSeries("s1", libraryId = libraryId))

    cache.put(SeriesStore.Key(userId, libraryId), listOf(series))

    repository.observeAllSeries().test {
      assertThat(awaitItem())
        .isEqualTo(listOf(series))

      assertThat(awaitItem())
        .single()
        .prop(Series::id)
        .isEqualTo("s1")
    }
  }

  @Test
  fun observeSeries_CachedWithSoTAndNetwork_EmitsCacheSoTNetwork() = runTest {
    val repository = createRepository(asTestDispatcherProvider())

    cache.put(SeriesStore.Key(userId, libraryId), listOf(series("c1", "Test Series")))
    databaseInterceptor.db
      .seriesQueries
      .insertOrIgnore(createDbSeries("s1", libraryId = libraryId))
    api.series = Result.success(listOf(createNetworkSeries("n1")))

    repository.observeAllSeries().test {
      assertThat(awaitItem())
        .single()
        .prop(Series::id)
        .isEqualTo("c1")

      assertThat(awaitItem())
        .single()
        .prop(Series::id)
        .isEqualTo("s1")

      assertThat(awaitItem()).all {
        index(0)
          .prop(Series::id)
          .isEqualTo("s1")
        index(1)
          .prop(Series::id)
          .isEqualTo("n1")
      }
    }
  }
}
