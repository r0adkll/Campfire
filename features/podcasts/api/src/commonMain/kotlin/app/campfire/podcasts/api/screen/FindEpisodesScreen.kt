package app.campfire.podcasts.api.screen

import app.campfire.common.screens.DetailScreen
import app.campfire.core.model.LibraryItemId
import app.campfire.core.parcelize.Parcelize

@Parcelize
data class FindEpisodesScreen(
  val libraryItemId: LibraryItemId,
) : DetailScreen(name = "FindEpisodes")
