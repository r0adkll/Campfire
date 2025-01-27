package app.campfire.android.attribution

import android.app.Application
import app.campfire.android.R
import app.campfire.core.attributions.LicenceAttributionLoader
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.util.withJson
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class AndroidLicenceAttributionLoader(
  private val application: Application,
  private val dispatcherProvider: DispatcherProvider,
) : LicenceAttributionLoader {

  private var cached: Libs? = null

  override suspend fun load(): Libs {
    if (cached != null) return cached!!
    return withContext(dispatcherProvider.io) {
      Libs.Builder()
        .withJson(application, R.raw.aboutlibraries)
        .build()
        .also { cached = it }
    }
  }
}
