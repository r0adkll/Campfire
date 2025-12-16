package app.campfire.account.api

data class AbsToken(
  val accessToken: String,
  val refreshToken: String?,
)
