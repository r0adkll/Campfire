package app.campfire.core.extensions

// TODO: Find iOS equivalent to UnknownHostException
actual val Throwable.isUnknownHostException: Boolean
  get() = message?.contains(NETWORK_ADDRESS_REGEX) == true ||
    this.cause?.isUnknownHostException ?: false
