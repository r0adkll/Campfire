#if (${PACKAGE_NAME} && ${PACKAGE_NAME} != "")package ${PACKAGE_NAME}

#end
#parse("File Header.java")
import app.campfire.CampfireDatabase
import app.campfire.core.coroutines.DispatcherProvider
import kotlinx.coroutines.withContext
import org.mobilenativefoundation.store.store5.SourceOfTruth

class ${NAME}SourceOfTruthFactory(
  private val db: CampfireDatabase,
  private val dispatcherProvider: DispatcherProvider,
) {

  fun create(): SourceOfTruth<${NAME}Store.Key, Unit /* TODO: Network Models */, ${NAME}Store.Output> {
    return SourceOfTruth.of(
      reader = { key ->
        TODO("Read db models for key")
      },
      writer = { key, shelves ->
        withContext(dispatcherProvider.databaseWrite) {
          TODO("Persist network models to disk")
        }
      },
      delete = { key ->
        withContext(dispatcherProvider.databaseWrite) {
          TODO("Delete db for key")
        }
      },
    )
  } 
}