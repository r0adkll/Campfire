package app.campfire.network.oidc

sealed class OpenIdException(message: String?) : Exception(message) {

  class AuthCancelled(message: String? = null) : OpenIdException(message)
  class AuthFailure(message: String? = null) : OpenIdException(message)
}
