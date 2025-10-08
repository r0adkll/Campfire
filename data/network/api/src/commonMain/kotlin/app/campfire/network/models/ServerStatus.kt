package app.campfire.network.models

import kotlinx.serialization.Serializable

@Serializable
data class ServerStatus(
  val app: String,
  val serverVersion: String,
  val isInit: Boolean,
  val language: String,
  val authMethods: List<String> = emptyList(),
  val authFormData: AuthFormData? = null,
)

@Serializable
data class AuthFormData(
  val authLoginCustomMessage: String? = null,
  val authOpenIDButtonText: String? = null,
  val authOpenIDAutoLaunch: Boolean = false,
)
