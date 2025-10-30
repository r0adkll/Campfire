package app.campfire.updates.source

/**
 * Implementations for this will live in the respective app platform modules
 */
interface AppUpdateSource {

  /**
   * Whether or not the user is signed-in to receive app updates. This is need for Beta
   * updates, but on production builds this will always be true
   */
  fun isSignedIn(): Boolean

  /**
   * Start the tester sign-in process on Beta builds. This is a no-op on production builds
   */
  suspend fun signIn()

  /**
   * Whether or not there is an update available for this user
   */
  suspend fun isUpdateAvailable(): Boolean

  /**
   * Get the latest available update
   */
  suspend fun getAvailableUpdate(): AppUpdate?

  /**
   * Kick of the update installation process
   */
  suspend fun installUpdate()
}

data class AppUpdate(
  val versionName: String,
  val versionCode: Long,
  val releaseNotes: String? = null,
)
