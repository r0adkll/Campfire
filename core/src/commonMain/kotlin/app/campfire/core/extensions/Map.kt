package app.campfire.core.extensions

inline fun <KeyT, ValueT> Map<KeyT, ValueT>.ifNotEmpty(block: Map<KeyT, ValueT>.() -> Unit) {
  if (isNotEmpty()) block() else Unit
}
