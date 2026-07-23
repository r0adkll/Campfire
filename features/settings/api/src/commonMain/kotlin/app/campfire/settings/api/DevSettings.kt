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

  /**
   * Debug-build only: the faked tester sign-in state used by the fake app update
   * source to test the update widget and flows.
   * Default: `true` so debug builds behave like a signed-in production build.
   */
  var fakeAppUpdateSignedIn: Boolean

  fun observeFakeAppUpdateSignedIn(): StateFlow<Boolean>

  /**
   * Debug-build only: when `true`, the fake app update source reports a faked
   * available release to test the update widget and details sheet.
   */
  var fakeAppUpdateAvailable: Boolean

  fun observeFakeAppUpdateAvailable(): StateFlow<Boolean>

  /**
   * Debug-build only: when `true`, simulated update downloads fail partway through
   * to test the failure/retry UX.
   */
  var fakeAppUpdateFailDownload: Boolean

  fun observeFakeAppUpdateFailDownload(): StateFlow<Boolean>
}
