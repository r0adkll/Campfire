package app.campfire.core.extensions

import java.net.UnknownHostException

actual val Throwable.isUnknownHostException: Boolean
  get() = this is UnknownHostException ||
    message?.contains(NETWORK_ADDRESS_REGEX) == true ||
    this.cause?.isUnknownHostException ?: false

private val NETWORK_ADDRESS_REGEX = "(http|ftp|https)://([\\w_-]+(?:\\.[\\w_-]+)+)([\\w.,@?^=%&:/~+#-]*[\\w@?^=%&/~+#-])".toRegex()
