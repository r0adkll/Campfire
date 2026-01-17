package app.campfire.core.extensions

expect val Throwable.isUnknownHostException: Boolean

internal val NETWORK_ADDRESS_REGEX =
  "(http|ftp|https)://([\\w_-]+(?:\\.[\\w_-]+)+)([\\w.,@?^=%&:/~+#-]*[\\w@?^=%&/~+#-])".toRegex()
