package app.campfire.network

class ApiException(
  val statusCode: Int,
  message: String,
) : Exception(message)

class AuthorizationException : Exception("Not valid login configuration found")
