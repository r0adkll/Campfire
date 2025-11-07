package app.campfire.core.attributions

import com.mikepenz.aboutlibraries.Libs

interface LicenseAttributionLoader {

  /**
   * Load the library and license attribution used in this app
   */
  suspend fun load(): Libs
}
