package app.campfire.network.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The type of media, will be book or podcast.
 *
 * Values: Book,Podcast
 */
@Serializable
enum class MediaType(val value: String) {

  @SerialName(value = "book")
  Book("book"),

  @SerialName(value = "podcastEpisode")
  Podcast("podcastEpisode"),
  ;

  /**
   * Override [toString()] to avoid using the enum variable name as the value, and instead use
   * the actual value defined in the API spec file.
   *
   * This solves a problem when the variable name and its value are different, and ensures that
   * the client sends the correct enum values to the server always.
   */
  override fun toString(): String = value

  companion object {
    /**
     * Converts the provided [data] to a [String] on success, null otherwise.
     */
    fun encode(data: Any?): String? = if (data is MediaType) "$data" else null

    /**
     * Returns a valid [MediaType] for [data], null otherwise.
     */
    fun decode(data: Any?): MediaType? = data?.let {
      val normalizedData = "$it".lowercase()
      values().firstOrNull { value ->
        it == value || normalizedData == "$value".lowercase()
      }
    }
  }
}
