package app.campfire.ui.theming.cache

interface DiskCache<V> : Cache<V> {

  /**
   * Load all cache entries, by key, from the disk
   */
  suspend fun selectAll(): Map<String, V>
}
