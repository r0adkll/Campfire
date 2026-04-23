package app.campfire.settings.api

import kotlin.time.Duration
import kotlinx.coroutines.flow.StateFlow

interface DevSettings {

  /**
   * Whether or not the developer mode has been enabled
   */
  var developerModeEnabled: Boolean

  fun observeDeveloperMode(): StateFlow<Boolean>

  /**
   * The minimum amount of time that a session can be re-used instead of generating a new
   * one. [Duration.ZERO] will result in a new session everytime.
   * Default: `10 minutes`
   */
  var sessionAge: Duration

  fun observeSessionAge(): StateFlow<Duration>

  /**
   * Observe the set of package names that have triggered skip-next / skip-previous media button
   * events on Android. Used by developer settings to surface unknown Bluetooth / remote control
   * package names so we can add them to our interception allow-list.
   */
  fun observeMediaButtonPackages(): StateFlow<Set<String>>

  /**
   * Record that a skip-next / skip-previous media button event was received from the given
   * [packageName]. Safe to call repeatedly with the same value; the stored set is deduplicated.
   */
  fun recordMediaButtonPackage(packageName: String)

  /**
   * Clear all recorded media button package names.
   */
  fun clearMediaButtonPackages()
}
