package app.campfire.network.oidc

data class OpenIdAuthorization(
  val codeVerifier: String,
  val code: String,
  val state: String,
)
