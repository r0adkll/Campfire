package app.campfire.podcasts.api

private val INVALID_FS_CHARS = setOf(':', '/', '\\', '*', '?', '"', '<', '>', '|')

/**
 * Strip filesystem-hostile characters from a podcast title for use in a server-side directory
 * name. The server validates that the final path is a sub-path of the destination folder, so we
 * defend the path-segment we contribute. Falls back to `"podcast"` when sanitization eats the
 * entire string.
 */
fun sanitizePodcastPathSegment(title: String): String {
  val sanitized = title.asSequence()
    .filterNot { it in INVALID_FS_CHARS }
    .filterNot { it.isISOControl() }
    .joinToString("")
    .trim()
  return sanitized.ifEmpty { "podcast" }
}
