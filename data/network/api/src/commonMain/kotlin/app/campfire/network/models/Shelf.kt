// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.models

import app.campfire.network.envelopes.Envelope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class Shelf : Envelope() {
  abstract val id: String
  abstract val label: String
  abstract val labelStringKey: String
  abstract val total: Int

  override fun applyPostage() = when (this) {
    is AuthorShelf -> entities.forEach { it.applyOrigin(origin) }
    is BookShelf -> entities.forEach { it.applyOrigin(origin) }
    is EpisodeShelf -> entities.forEach { it.applyOrigin(origin) }
    is PodcastShelf -> entities.forEach { it.applyOrigin(origin) }
    is SeriesShelf -> entities.forEach { it.applyOrigin(origin) }
  }

  @Serializable
  @SerialName("book")
  data class BookShelf(
    override val id: String,
    override val label: String,
    override val labelStringKey: String,
    override val total: Int,
    val entities: List<LibraryItemMinified.Book>,
  ) : Shelf()

  @Serializable
  @SerialName("podcast")
  data class PodcastShelf(
    override val id: String,
    override val label: String,
    override val labelStringKey: String,
    override val total: Int,
    val entities: List<LibraryItemMinified.Podcast>,
  ) : Shelf()

  /**
   * Episode shelf items reuse the [LibraryItemMinified.Podcast] shape, with `recentEpisode`
   * populated identifying the specific episode the shelf is highlighting (e.g. for
   * `episodes-recently-added`).
   */
  @Serializable
  @SerialName("episode")
  data class EpisodeShelf(
    override val id: String,
    override val label: String,
    override val labelStringKey: String,
    override val total: Int,
    val entities: List<LibraryItemMinified.Podcast>,
  ) : Shelf()

  @Serializable
  @SerialName("series")
  data class SeriesShelf(
    override val id: String,
    override val label: String,
    override val labelStringKey: String,
    override val total: Int,
    val entities: List<SeriesPersonalized>,
  ) : Shelf()

  @Serializable
  @SerialName("authors")
  data class AuthorShelf(
    override val id: String,
    override val label: String,
    override val labelStringKey: String,
    override val total: Int,
    val entities: List<Author>,
  ) : Shelf()
}
