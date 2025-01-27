package app.campfire.android.attribution

import android.app.Application
import app.campfire.android.R
import app.campfire.core.attributions.LicenseAttributionLoader
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.util.withJson
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject

@ContributesBinding(AppScope::class)
@Inject
class AndroidLicenseAttributionLoader(
  private val application: Application,
  private val dispatcherProvider: DispatcherProvider,
) : LicenseAttributionLoader {

  override suspend fun load(): Libs {
    return withContext(dispatcherProvider.io) {
      Libs.Builder()
        .withJson(application, R.raw.aboutlibraries)
        .build()
    }
  }
}
