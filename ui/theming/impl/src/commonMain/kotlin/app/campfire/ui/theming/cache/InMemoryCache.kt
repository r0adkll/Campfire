package app.campfire.ui.theming.cache

import co.touchlab.stately.collections.ConcurrentMutableMap

/**
 * A simple in-memory cache implementation that is backed by a
 * [ConcurrentMutableMap] for thread-safety.
 */
class InMemoryCache<V> : Cache<V> {
  private val cache = ConcurrentMutableMap<String, V>()

  override suspend fun get(key: String): V? {
    return cache[key]
  }

  override suspend fun set(key: String, value: V) {
    cache[key] = value
  }

  override suspend fun putAll(from: Map<String, V>) {
    cache.putAll(from)
  }

  override suspend fun remove(key: String) {
    cache.remove(key)
  }

  override suspend fun containsKey(key: String): Boolean {
    return cache.containsKey(key)
  }
}
