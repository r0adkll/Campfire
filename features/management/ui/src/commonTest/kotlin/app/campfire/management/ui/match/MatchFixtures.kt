// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.management.ui.match

import androidx.compose.foundation.text.input.TextFieldState
import app.campfire.management.api.match.MatchProvider
import kotlin.time.Duration

fun matchUiState(
  search: MatchSearch = MatchSearch.NotSearched,
  review: MatchReviewState? = null,
  provider: MatchProvider? = FakeMetadataMatchRepository.Audible,
  bookDuration: Duration? = null,
  bookAsin: String? = null,
  canChangeCover: Boolean = true,
  eventSink: (MatchUiEvent) -> Unit = {},
) = MatchUiState(
  providers = listOf(FakeMetadataMatchRepository.Audible, FakeMetadataMatchRepository.Google),
  provider = provider,
  title = TextFieldState(),
  author = TextFieldState(),
  search = search,
  bookDuration = bookDuration,
  bookAsin = bookAsin,
  review = review,
  canChangeCover = canChangeCover,
  eventSink = eventSink,
)
