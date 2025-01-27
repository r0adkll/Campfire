package app.campfire.attribution

import app.campfire.core.attributions.LicenceAttributionLoader
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.util.withJson
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.withContext

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class DesktopLicenseAttributionLoader(
  private val dispatcherProvider: DispatcherProvider,
) : LicenceAttributionLoader {

  private var cached: Libs? = null

  override suspend fun load(): Libs {
    if (cached != null) return cached!!
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
