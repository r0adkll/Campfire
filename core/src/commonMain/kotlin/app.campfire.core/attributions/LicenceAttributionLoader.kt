package app.campfire.core.attributions

import com.mikepenz.aboutlibraries.Libs

interface LicenceAttributionLoader {

  /**
   * Load the library and license attribution used in this app
   */
  suspend fun load(): Libs
}
