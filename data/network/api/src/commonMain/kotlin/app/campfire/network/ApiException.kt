package app.campfire.network

class ApiException(
  statusCode: Int,
  message: String = "",
) : Exception("API Error [$statusCode]: $message")

class AuthorizationException : Exception("Not valid login configuration found")
