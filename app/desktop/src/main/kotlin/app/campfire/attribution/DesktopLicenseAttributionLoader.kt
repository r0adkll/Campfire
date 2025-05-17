package app.campfire.attribution

import app.campfire.core.attributions.LicenseAttributionLoader
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.util.withJson
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.withContext

@ContributesBinding(AppScope::class)
@Inject
class DesktopLicenseAttributionLoader(
  private val dispatcherProvider: DispatcherProvider,
) : LicenseAttributionLoader {

  override suspend fun load(): Libs {
    return withContext(dispatcherProvider.io) {
      Libs.Builder()
        .withJson(readResourceFile())
        .build()
    }
  }

  private fun readResourceFile(): ByteArray {
    val inputStream = DesktopLicenseAttributionLoader::class.java.getResourceAsStream("/aboutlibraries.json")
    return inputStream.use { fis ->
      fis!!.readAllBytes()
    }
  }
}
