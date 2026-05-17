package app.campfire.podcasts.api.screen

import app.campfire.common.screens.BaseScreen
import app.campfire.core.model.LibraryId
import app.campfire.core.parcelize.Parcelize

/**
 * Entry point of the "Add podcast" flow. Renders a single search field that either searches the
 * server's iTunes proxy or — when the input looks like an HTTP URL — fetches and previews the
 * pasted RSS feed. Selecting a result pushes [AddPodcastBuilderScreen].
 */
@Parcelize
data class AddPodcastScreen(
  val libraryId: LibraryId,
) : BaseScreen(name = "AddPodcast")
