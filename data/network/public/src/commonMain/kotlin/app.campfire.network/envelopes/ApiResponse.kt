package app.campfire.network.envelopes

/**
 * An API response envelope that contains the server URL that was used for the current request.
 */
class ApiResponse<T>(
  val data: T,
  val serverUrl: String,
)
