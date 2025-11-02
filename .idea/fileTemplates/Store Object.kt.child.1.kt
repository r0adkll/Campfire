#if (${PACKAGE_NAME} && ${PACKAGE_NAME} != "")package ${PACKAGE_NAME}

#end
#parse("File Header.java")
import app.campfire.data.mapping.asFetcherResult
import app.campfire.network.AudioBookShelfApi
import app.campfire.network.models.Shelf
import org.mobilenativefoundation.store.store5.Fetcher

class ${NAME}FetcherFactory(
  private val api: AudioBookShelfApi,
) {

  fun create(): Fetcher<${NAME}Store.Key, Unit /* TODO: Network Model */> {
    return Fetcher.ofResult { key ->
      TODO("Implement API")
    }
  }
}
