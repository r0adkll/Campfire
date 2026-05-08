package app.campfire.common.compose.extensions

const val CampfireTimestampScheme = "campfire-timestamp:"

private val LeadingLiteralNewlineRegex = "^(\\\\n)+".toRegex()
private val UrlRegex = Regex("""https?://[^\s<>"']+""")
private val UrlTrailingPunct = Regex("""[)\].,;:!?'"]+$""")
private val TimestampRegex = Regex("""\b(?:(\d{1,2}):)?(\d{1,2}):(\d{2})\b""")

/**
 * Wraps bare http(s) URLs in `<a href>` tags so they render as clickable links via
 * the richeditor's built-in [androidx.compose.ui.platform.LocalUriHandler] dispatch.
 * Content inside existing `<a>...</a>` blocks is left untouched so we don't double-wrap.
 */
fun String.linkify(): String = replaceOutsideAnchors { it.wrapUrls() }

/**
 * Wraps `HH:MM:SS` / `MM:SS` timestamps in `<a href="$CampfireTimestampScheme<seconds>">`
 * anchors. Only timestamps in plain text (outside existing `<a>` blocks) are wrapped, so
 * timestamps inside URL paths or query strings stay untouched. The href payload is the
 * total seconds — handlers can parse with `String.removePrefix(CampfireTimestampScheme).toInt()`.
 */
fun String.linkifyTimestamps(): String = replaceOutsideAnchors { it.wrapTimestamps() }

/**
 * Normalizes a raw HTML-ish description string for rendering with the richeditor:
 * strips leading literal "\n" artifacts (escaped newlines that survived JSON decoding),
 * linkifies bare URLs, and converts remaining newlines to `<br>` tags.
 */
fun String.toRichTextHtml(): String =
  trim()
    .replace(LeadingLiteralNewlineRegex, "")
    .linkify()
    .replace("\n", "<br>")

private fun String.wrapUrls(): String =
  UrlRegex.replace(this) { match ->
    val raw = match.value
    val trail = UrlTrailingPunct.find(raw)?.value.orEmpty()
    val url = raw.dropLast(trail.length)
    """<a href="$url">$url</a>$trail"""
  }

private fun String.wrapTimestamps(): String =
  TimestampRegex.replace(this) { match ->
    val (h, m, s) = match.destructured
    val hours = h.toIntOrNull() ?: 0
    val minutes = m.toIntOrNull() ?: 0
    val seconds = s.toIntOrNull() ?: 0
    val total = hours * 3600 + minutes * 60 + seconds
    """<a href="$CampfireTimestampScheme$total">${match.value}</a>"""
  }

private inline fun String.replaceOutsideAnchors(transform: (String) -> String): String {
  val builder = StringBuilder(length + 32)
  var i = 0
  var anchorDepth = 0
  while (i < length) {
    if (this[i] == '<') {
      val end = indexOf('>', i)
      if (end == -1) {
        builder.append(this, i, length)
        return builder.toString()
      }
      val tag = substring(i, end + 1)
      builder.append(tag)
      val lower = tag.lowercase()
      when {
        lower.startsWith("</a") -> if (anchorDepth > 0) anchorDepth--
        lower.startsWith("<a ") || lower == "<a>" -> anchorDepth++
      }
      i = end + 1
    } else {
      val nextTag = indexOf('<', i).let { if (it == -1) length else it }
      val segment = substring(i, nextTag)
      builder.append(if (anchorDepth > 0) segment else transform(segment))
      i = nextTag
    }
  }
  return builder.toString()
}
