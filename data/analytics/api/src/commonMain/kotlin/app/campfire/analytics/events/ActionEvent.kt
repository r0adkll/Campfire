package app.campfire.analytics.events

import kotlin.jvm.JvmInline

/**
 * When the user clicks a button, or action, in the app fire this event
 * @param obj the source of the action. i.e. "Author"
 * @param noun the subject of the action, if exists. i.e. "Series", "Audiobook", etc
 */
class ActionEvent(
  obj: String,
  verb: String,
  noun: Any? = null,
  extras: Map<String, Any>? = null,
) : AnalyticEvent(
  eventName = "${obj}_$verb",
  params = buildMap {
    noun?.let { put("noun", noun) }
    extras?.let { putAll(it) }
  },
)

fun ActionEvent(
  obj: String,
  verb: Verb,
  noun: Any? = null,
  extras: Map<String, Any>? = null,
) = ActionEvent(obj, verb.value, noun, extras)

@JvmInline
value class Verb(val value: String)

val Viewed get() = Verb("viewed")
val Selected get() = Verb("selected")
val Click get() = Verb("clicked")

val Created get() = Verb("created")
val Updated get() = Verb("updated")
val Deleted get() = Verb("deleted")

@Suppress("FunctionName")
fun ContentSelected(
  contentType: ContentType,
  noun: String? = null,
) = ActionEvent(contentType.value, "selected", noun)

enum class ContentType(internal val value: String) {
  LibraryItem("library_item"),
  Series("series"),
  Collection("collection"),
  Author("author"),
  Narrator("narrator"),
  Tag("tag"),
  Genre("genre"),
}
