// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.podcasts.api

import app.campfire.core.parcelize.Parcelable
import app.campfire.core.parcelize.Parcelize

/**
 * The data the "Add podcast" builder operates on. Source-agnostic: both an iTunes search hit and
 * a pasted RSS URL converge on this shape. The builder may edit [title], [author], and
 * [descriptionPlain] before submitting.
 *
 * [feedUrl] is the only field strictly required for creation; the server tolerates the rest being
 * sparse but they meaningfully improve the entry on first display.
 */
@Parcelize
data class PodcastDraft(
  val title: String,
  val author: String?,
  val descriptionHtml: String?,
  val descriptionPlain: String?,
  val coverUrl: String?,
  val feedUrl: String,
  val itunesId: String?,
  val itunesArtistId: String?,
  val itunesPageUrl: String?,
  val releaseDateIso: String?,
  val language: String?,
  val genres: List<String>,
  val explicit: Boolean,
  val episodeType: String? = null,
) : Parcelable
