package app.campfire.ios.attribution

import app.campfire.core.attributions.LicenseAttributionLoader
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import campfire.app.ios.generated.resources.Res
import com.mikepenz.aboutlibraries.Libs
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject
import org.jetbrains.compose.resources.ExperimentalResourceApi

@ContributesBinding(AppScope::class)
@Inject
class IosLicenseAttributionLoader(
  private val dispatcherProvider: DispatcherProvider,
) : LicenseAttributionLoader {

  override suspend fun load(): Libs = withContext(dispatcherProvider.io) {
    Libs.Builder()
      .withJson(readResourceFile())
      .build()
  }

  @OptIn(ExperimentalResourceApi::class)
  private suspend fun readResourceFile(): String {
    return Res.readBytes("files/aboutlibraries.json").decodeToString()
  }
}
