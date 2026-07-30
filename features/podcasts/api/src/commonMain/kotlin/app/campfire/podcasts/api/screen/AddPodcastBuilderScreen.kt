// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.podcasts.api.screen

import app.campfire.common.screens.BaseScreen
import app.campfire.common.screens.Presentation
import app.campfire.core.model.LibraryId
import app.campfire.core.parcelize.Parcelize
import app.campfire.podcasts.api.PodcastDraft

/**
 * Confirmation step of the "Add podcast" flow. Shows the pre-filled [draft] with editable
 * title/author/description fields, the folder picker, and the auto-download toggle. Submitting
 * sends `POST /api/podcasts` and pops the back stack to [app.campfire.libraries.api.screen.LibraryScreen].
 */
@Parcelize
data class AddPodcastBuilderScreen(
  val libraryId: LibraryId,
  val draft: PodcastDraft,
) : BaseScreen(name = "AddPodcastBuilder") {
  override val presentation: Presentation
    get() = Presentation(hideBottomNav = true, hidePlaybackBar = true)
}
