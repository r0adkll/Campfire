// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.hardcover.graphql

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

private const val BOOK_SELECTION = """
      id
      slug
      title
      rating
      ratings_count
      ratings_distribution
      reviews_count
      release_date
      cached_image
"""

/**
 * Builds a single lookup over every identifier the item carries — identifier
 * coverage on Hardcover editions is uneven (audiobook ASINs especially), so one
 * `_or` across all of them beats sequential per-identifier lookups. GraphQL
 * requires every declared variable to be used, so the declaration list and the
 * predicate list are built together.
 */
internal fun bookByIdentifiersQuery(hasIsbn: Boolean, hasAsin: Boolean): String {
  require(hasIsbn || hasAsin) { "At least one identifier is required" }
  val variables = buildList {
    if (hasIsbn) add("${'$'}isbn: String!")
    if (hasAsin) add("${'$'}asin: String!")
  }.joinToString(", ")
  val predicates = buildList {
    if (hasIsbn) {
      add("{isbn_13: {_eq: ${'$'}isbn}}")
      add("{isbn_10: {_eq: ${'$'}isbn}}")
    }
    if (hasAsin) {
      add("{asin: {_eq: ${'$'}asin}}")
    }
  }.joinToString(", ")
  return """
  query BookByIdentifiers($variables) {
    books(
      where: {editions: {_or: [$predicates]}}
      order_by: {users_count: desc}
      limit: 1
    ) {$BOOK_SELECTION}
  }
  """.trimIndent()
}

internal val BOOK_REVIEWS_QUERY = """
  query BookReviews(${'$'}bookId: Int!, ${'$'}limit: Int!) {
    user_books(
      where: {book_id: {_eq: ${'$'}bookId}, has_review: {_eq: true}}
      limit: ${'$'}limit
    ) {
      rating
      review
      review_has_spoilers
      user {
        username
        name
        flair
        cached_image
      }
    }
  }
""".trimIndent()

internal val ME_QUERY = """
  query Me {
    me {
      username
    }
  }
""".trimIndent()

@Serializable
internal data class BooksData(
  val books: List<HardcoverBook> = emptyList(),
)

@Serializable
internal data class HardcoverBook(
  val id: Long,
  val slug: String? = null,
  val title: String? = null,
  val rating: Double? = null,
  @SerialName("ratings_count") val ratingsCount: Int? = null,
  @SerialName("ratings_distribution") val ratingsDistribution: JsonElement? = null,
  @SerialName("reviews_count") val reviewsCount: Int? = null,
  @SerialName("release_date") val releaseDate: String? = null,
  @SerialName("cached_image") val cachedImage: JsonElement? = null,
)

@Serializable
internal data class UserBooksData(
  @SerialName("user_books") val userBooks: List<HardcoverUserBook> = emptyList(),
)

@Serializable
internal data class HardcoverUserBook(
  val rating: Double? = null,
  val review: String? = null,
  @SerialName("review_has_spoilers") val hasSpoilers: Boolean? = null,
  val user: HardcoverUser? = null,
)

@Serializable
internal data class HardcoverUser(
  val username: String? = null,
  val name: String? = null,
  val flair: String? = null,
  @SerialName("cached_image") val cachedImage: JsonElement? = null,
)

/**
 * `ratings_distribution` is a jsonb column with no stable documented shape;
 * tolerate the object (`{"1": 4}`) and array (`[{"rating": 1, "count": 4}]`)
 * encodings and give up quietly on anything else.
 */
internal fun parseRatingsDistribution(element: JsonElement?): Map<Int, Int>? {
  return when (element) {
    is JsonObject -> element.entries.mapNotNull { (key, value) ->
      val star = key.toIntOrNull() ?: return@mapNotNull null
      val count = value.jsonPrimitive.intOrNull ?: return@mapNotNull null
      star to count
    }.toMap().ifEmpty { null }

    is JsonArray -> element.mapNotNull { entry ->
      val obj = entry as? JsonObject ?: return@mapNotNull null
      val star = obj["rating"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
      val count = obj["count"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
      star to count
    }.toMap().ifEmpty { null }

    else -> null
  }
}

/** `cached_image` is a jsonb blob shaped like `{"url": "...", ...}`. */
internal fun parseCoverUrl(element: JsonElement?): String? {
  val obj = element as? JsonObject ?: return null
  return obj["url"]?.jsonPrimitive?.content?.takeUnless { it.isBlank() }
}

@Serializable
internal data class MeData(
  val me: JsonElement? = null,
)

/** `me` has been observed as both a single object and a one-element list. */
internal fun parseUsername(me: JsonElement?): String? = when (me) {
  is JsonObject -> me["username"]?.jsonPrimitive?.content
  is JsonArray -> (me.firstOrNull() as? JsonObject)?.get("username")?.jsonPrimitive?.content
  else -> null
}
