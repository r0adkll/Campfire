package app.campfire.core.extensions

import java.net.UnknownHostException

actual val Throwable.isUnknownHostException: Boolean
  get() = this is UnknownHostException ||
    message?.contains(NETWORK_ADDRESS_REGEX) == true ||
    this.cause?.isUnknownHostException ?: false
