package app.campfire.ui.theming.cache

/**
 * A generic cache interface for storing theming related information
 */
interface Cache<V> {
  suspend operator fun get(key: String): V?
  suspend operator fun set(key: String, value: V)
  suspend fun putAll(from: Map<String, V>)
  suspend fun remove(key: String)
  suspend fun containsKey(key: String): Boolean
}
