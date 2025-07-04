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
}
