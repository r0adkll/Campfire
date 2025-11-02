#if (${PACKAGE_NAME} && ${PACKAGE_NAME} != "")package ${PACKAGE_NAME}

#end
#parse("File Header.java")
import app.campfire.CampfireDatabase
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.logging.Cork
import app.campfire.network.AudioBookShelfApi
import kotlin.time.Duration.Companion.minutes
import me.tatarka.inject.annotations.Inject
import org.mobilenativefoundation.store.store5.MemoryPolicy
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreBuilder

object ${NAME}Store : Cork {

  override val tag = "${NAME}Store"
  
  @Inject
  class Factory(
    api: AudioBookShelfApi,
    db: CampfireDatabase,
    dispatcherProvider: DispatcherProvider,
  ) {
  
    private val fetcherFactory = ${NAME}FetcherFactory(api)
    private val sourceOfTruthFactory = ${NAME}SourceOfTruthFactory(db, dispatcherProvider)
    
    fun create(): Store<Key, Output> {
      return StoreBuilder
        .from(
          fetcher = fetcherFactory.create(),
          sourceOfTruth = sourceOfTruthFactory.create(),
        )
        .cachePolicy(
          MemoryPolicy.builder<Key, Output>()
            .setExpireAfterAccess(5.minutes)
            .build(),
        )
        .build()
    }
  }
  
  sealed interface Key
  
  sealed interface Output
}