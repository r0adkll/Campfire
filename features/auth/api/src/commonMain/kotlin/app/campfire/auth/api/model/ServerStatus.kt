package app.campfire.auth.api.model

data class ServerStatus(
  val serverVersion: String,
  val isInit: Boolean,
  val language: String,
  val authMethods: List<String> = emptyList(),
  val authFormData: OpenIdFormData? = null,
)

data class OpenIdFormData(
  val customMessage: String? = null,
  val openIdButtonText: String? = null,
)

const val AUTH_METHOD_OPENID = "openid"
const val AUTH_METHOD_LOCAL = "local"
