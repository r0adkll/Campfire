package app.campfire.network.envelopes

import app.campfire.network.models.ServerSettings
import app.campfire.network.models.User
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
  val username: String,
  val password: String,
)

@Serializable
data class LoginResponse(
  val user: User,
  val userDefaultLibraryId: String,
  val serverSettings: ServerSettings,
  @SerialName("Source") val source: String,
)
