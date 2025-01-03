package app.campfire.network

/**
 * The server origin of the request
 */
sealed interface RequestOrigin {
  data class Url(val serverUrl: String) : RequestOrigin
  data object None : RequestOrigin
}
