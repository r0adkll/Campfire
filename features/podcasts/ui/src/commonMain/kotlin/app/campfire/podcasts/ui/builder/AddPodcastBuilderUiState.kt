// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.podcasts.ui.builder

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Immutable
import app.campfire.libraries.api.LibraryFolder
import app.campfire.podcasts.api.RemotePodcastEpisode
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

@Immutable
data class AddPodcastBuilderUiState(
  val titleState: TextFieldState,
  val authorState: TextFieldState,
  val descriptionState: TextFieldState,
  val coverUrl: String?,
  /** Stable id matching the search/preview source so the cover animates in via SharedElement. */
  val sharedTransitionKey: String,
  val foldersState: FoldersState,
  val pathPreview: String?,
  val episodeType: EpisodeType,
  val explicitEnabled: Boolean,
  val autoDownloadEnabled: Boolean,
  val feedState: FeedState,
  val selectedEpisodeUrls: Set<String>,
  val isSubmitting: Boolean,
  val submitError: SubmitError?,
  val eventSink: (AddPodcastBuilderUiEvent) -> Unit,
) : CircuitUiState

/**
 * Background fetch of the RSS feed used to (a) hydrate the description/episode-type when iTunes
 * leaves them empty and (b) drive the episode-picker section. The form is usable while [Loading].
 */
sealed interface FeedState {
  data object Loading : FeedState
  data object Error : FeedState
  data class Loaded(val episodes: List<RemotePodcastEpisode>) : FeedState
}

/**
 * The iTunes-style episode order. [Default] tells the server to use its own default (typically
 * [Episodic]). [serialKey] is the wire value passed in `media.metadata.type`.
 */
enum class EpisodeType(val serialKey: String) {
  Episodic(serialKey = "episodic"),
  Serial(serialKey = "serial"),
  ;

  companion object {
    val Default get() = Episodic
    fun fromSerialKey(value: String?): EpisodeType = when (value?.lowercase()) {
      "episodic" -> Episodic
      "serial" -> Serial
      else -> Default
    }
  }
}

sealed interface FoldersState {
  data object Loading : FoldersState
  data object Error : FoldersState
  data class Loaded(
    val folders: List<LibraryFolder>,
    val selectedId: String,
  ) : FoldersState
}

sealed interface SubmitError {
  data object Forbidden : SubmitError
  data object PathConflict : SubmitError
  data object Generic : SubmitError
}

sealed interface AddPodcastBuilderUiEvent : CircuitUiEvent {
  data object Back : AddPodcastBuilderUiEvent
  data class FolderSelected(val folderId: String) : AddPodcastBuilderUiEvent
  data class AutoDownloadToggled(val enabled: Boolean) : AddPodcastBuilderUiEvent
  data class ExplicitToggled(val enabled: Boolean) : AddPodcastBuilderUiEvent
  data class EpisodeTypeSelected(val type: EpisodeType) : AddPodcastBuilderUiEvent
  data class EpisodeSelectionToggled(val enclosureUrl: String) : AddPodcastBuilderUiEvent
  data object SelectAllEpisodes : AddPodcastBuilderUiEvent
  data object ClearEpisodeSelection : AddPodcastBuilderUiEvent
  data object RetryFeed : AddPodcastBuilderUiEvent
  data object Submit : AddPodcastBuilderUiEvent
  data object RetryFolders : AddPodcastBuilderUiEvent
  data object DismissError : AddPodcastBuilderUiEvent
}
