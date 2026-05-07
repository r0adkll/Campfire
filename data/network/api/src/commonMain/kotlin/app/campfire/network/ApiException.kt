package app.campfire.network

class ApiException(
  val statusCode: Int,
  val apiMessage: String = "",
) : Exception("API Error [$statusCode]: $apiMessage")

class AuthorizationException : Exception("Not valid login configuration found")
