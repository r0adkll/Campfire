package app.campfire.podcasts.ui.builder

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.insert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.ActionEvent
import app.campfire.analytics.events.Click
import app.campfire.common.compose.util.rememberRetainedCoroutineScope
import app.campfire.core.di.UserScope
import app.campfire.libraries.api.LibraryRepository
import app.campfire.podcasts.api.AddPodcastException
import app.campfire.podcasts.api.PodcastDraft
import app.campfire.podcasts.api.PodcastsRepository
import app.campfire.podcasts.api.mergedWith
import app.campfire.podcasts.api.sanitizePodcastPathSegment
import app.campfire.podcasts.api.screen.AddPodcastBuilderScreen
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.foundation.NonPausablePresenter
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.retained.rememberRetainedSaveable
import com.slack.circuit.runtime.Navigator
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@CircuitInject(AddPodcastBuilderScreen::class, UserScope::class)
@Inject
class AddPodcastBuilderPresenter(
  @Assisted private val screen: AddPodcastBuilderScreen,
  @Assisted private val navigator: Navigator,
  private val analytics: Analytics,
  private val libraryRepository: LibraryRepository,
  private val podcastsRepository: PodcastsRepository,
) : NonPausablePresenter<AddPodcastBuilderUiState> {

  @Composable
  override fun present(): AddPodcastBuilderUiState {
    val scope = rememberRetainedCoroutineScope()

    val titleState = rememberRetained { TextFieldState(screen.draft.title) }
    val authorState = rememberRetained { TextFieldState(screen.draft.author.orEmpty()) }
    val descriptionState = rememberRetained {
      TextFieldState(screen.draft.descriptionPlain.orEmpty())
    }

    var autoDownloadEnabled by rememberRetainedSaveable { mutableStateOf(true) }
    var explicitEnabled by rememberRetainedSaveable { mutableStateOf(screen.draft.explicit) }
    var episodeType by rememberRetainedSaveable {
      mutableStateOf(EpisodeType.fromSerialKey(screen.draft.episodeType))
    }
    var foldersState: FoldersState by remember { mutableStateOf(FoldersState.Loading) }
    var isSubmitting by remember { mutableStateOf(false) }
    var submitError: SubmitError? by remember { mutableStateOf(null) }
    var foldersRetryToken by remember { mutableStateOf(0) }

    var feedState: FeedState by remember { mutableStateOf(FeedState.Loading) }
    var feedDraft: PodcastDraft? by remember { mutableStateOf(null) }
    var selectedEpisodeUrls by rememberRetainedSaveable { mutableStateOf(emptySet<String>()) }
    var feedRetryToken by remember { mutableStateOf(0) }
    var hasHydrated by remember { mutableStateOf(false) }

    val pathPreview: String? by remember {
      derivedStateOf {
        val loaded = foldersState as? FoldersState.Loaded ?: return@derivedStateOf null
        val folder = loaded.folders.find { it.id == loaded.selectedId }
          ?: return@derivedStateOf null
        val title = titleState.text.toString().trim().ifEmpty { screen.draft.title }
        "${folder.fullPath.trimEnd('/')}/${sanitizePodcastPathSegment(title)}"
      }
    }

    LaunchedEffect(screen.libraryId, foldersRetryToken) {
      foldersState = FoldersState.Loading
      libraryRepository.getAddPodcastContext(screen.libraryId).fold(
        onSuccess = { ctx ->
          foldersState = if (ctx.folders.isEmpty()) {
            FoldersState.Error
          } else {
            FoldersState.Loaded(ctx.folders, selectedId = ctx.folders.first().id)
          }
        },
        onFailure = { foldersState = FoldersState.Error },
      )
    }

    LaunchedEffect(screen.draft.feedUrl, feedRetryToken) {
      feedState = FeedState.Loading
      podcastsRepository.fetchPodcastFeedDetails(screen.draft.feedUrl).fold(
        onSuccess = { details ->
          feedState = FeedState.Loaded(details.episodes)
          feedDraft = details.draft
          if (!hasHydrated) {
            hydrateFromFeed(
              feed = details.draft,
              descriptionState = descriptionState,
              currentEpisodeType = episodeType,
              setEpisodeType = { episodeType = it },
            )
            hasHydrated = true
          }
        },
        onFailure = { feedState = FeedState.Error },
      )
    }

    return AddPodcastBuilderUiState(
      titleState = titleState,
      authorState = authorState,
      descriptionState = descriptionState,
      coverUrl = screen.draft.coverUrl ?: feedDraft?.coverUrl,
      sharedTransitionKey = screen.draft.feedUrl,
      foldersState = foldersState,
      pathPreview = pathPreview,
      episodeType = episodeType,
      explicitEnabled = explicitEnabled,
      autoDownloadEnabled = autoDownloadEnabled,
      feedState = feedState,
      selectedEpisodeUrls = selectedEpisodeUrls,
      isSubmitting = isSubmitting,
      submitError = submitError,
      eventSink = { event ->
        when (event) {
          AddPodcastBuilderUiEvent.Back -> navigator.pop()
          AddPodcastBuilderUiEvent.DismissError -> {
            submitError = null
          }
          AddPodcastBuilderUiEvent.RetryFolders -> {
            foldersRetryToken++
          }
          AddPodcastBuilderUiEvent.RetryFeed -> {
            feedRetryToken++
          }
          is AddPodcastBuilderUiEvent.FolderSelected -> {
            val loaded = foldersState as? FoldersState.Loaded ?: return@AddPodcastBuilderUiState
            foldersState = loaded.copy(selectedId = event.folderId)
          }
          is AddPodcastBuilderUiEvent.AutoDownloadToggled -> {
            autoDownloadEnabled = event.enabled
          }
          is AddPodcastBuilderUiEvent.ExplicitToggled -> {
            explicitEnabled = event.enabled
          }
          is AddPodcastBuilderUiEvent.EpisodeTypeSelected -> {
            episodeType = event.type
          }
          is AddPodcastBuilderUiEvent.EpisodeSelectionToggled -> {
            selectedEpisodeUrls = if (event.enclosureUrl in selectedEpisodeUrls) {
              selectedEpisodeUrls - event.enclosureUrl
            } else {
              selectedEpisodeUrls + event.enclosureUrl
            }
          }
          AddPodcastBuilderUiEvent.SelectAllEpisodes -> {
            val loaded = feedState as? FeedState.Loaded ?: return@AddPodcastBuilderUiState
            selectedEpisodeUrls = loaded.episodes.mapTo(mutableSetOf()) { it.enclosureUrl }
          }
          AddPodcastBuilderUiEvent.ClearEpisodeSelection -> {
            selectedEpisodeUrls = emptySet()
          }
          AddPodcastBuilderUiEvent.Submit -> {
            val loaded = foldersState as? FoldersState.Loaded ?: return@AddPodcastBuilderUiState
            val selectedFolder = loaded.folders.find { it.id == loaded.selectedId }
              ?: return@AddPodcastBuilderUiState
            if (isSubmitting) return@AddPodcastBuilderUiState

            val editedDraft = screen.draft.copy(
              title = titleState.text.toString().trim().ifEmpty { screen.draft.title },
              author = authorState.text.toString().trim().ifEmpty { null },
              descriptionPlain = descriptionState.text.toString().trim().ifEmpty { null },
              explicit = explicitEnabled,
              episodeType = episodeType.serialKey,
            )
            // Fold in any feed-only fields (cover, language, genres) iTunes didn't provide.
            val draft = feedDraft?.let { editedDraft.mergedWith(it) } ?: editedDraft

            val episodesToQueue = (feedState as? FeedState.Loaded)?.episodes
              ?.filter { it.enclosureUrl in selectedEpisodeUrls }
              .orEmpty()

            analytics.send(ActionEvent("add_podcast_submit", Click))
            isSubmitting = true
            submitError = null
            scope.launch {
              podcastsRepository.addPodcast(
                libraryId = screen.libraryId,
                folder = selectedFolder,
                draft = draft,
                autoDownloadEpisodes = autoDownloadEnabled,
              ).fold(
                onSuccess = { newId ->
                  analytics.send(ActionEvent("add_podcast_success", Click))
                  if (episodesToQueue.isNotEmpty()) {
                    // Best-effort: a failure here doesn't undo the podcast creation. The user can
                    // re-queue via the Find Episodes flow if needed.
                    podcastsRepository.queueEpisodeDownloads(newId, episodesToQueue)
                  }
                  isSubmitting = false
                  navigator.pop()
                  navigator.pop()
                },
                onFailure = { error ->
                  isSubmitting = false
                  submitError = error.toSubmitError()
                },
              )
            }
          }
        }
      },
    )
  }
}

/**
 * Fill in fields that the iTunes search hit left empty. Only touches each field if the user
 * hasn't already typed/picked something: the description is hydrated when blank, and the episode
 * order is hydrated when still [EpisodeType.Default].
 */
private fun hydrateFromFeed(
  feed: PodcastDraft,
  descriptionState: TextFieldState,
  currentEpisodeType: EpisodeType,
  setEpisodeType: (EpisodeType) -> Unit,
) {
  if (descriptionState.text.isBlank()) {
    val feedDescription = feed.descriptionPlain?.takeIf { it.isNotBlank() }
    if (feedDescription != null) {
      descriptionState.clearText()
      descriptionState.edit { insert(0, feedDescription) }
    }
  }
  if (currentEpisodeType == EpisodeType.Default && !feed.episodeType.isNullOrBlank()) {
    setEpisodeType(EpisodeType.fromSerialKey(feed.episodeType))
  }
}

private fun Throwable.toSubmitError(): SubmitError {
  return when ((this as? AddPodcastException)?.kind) {
    AddPodcastException.Kind.Forbidden -> SubmitError.Forbidden
    AddPodcastException.Kind.PathConflict -> SubmitError.PathConflict
    AddPodcastException.Kind.Generic, null -> SubmitError.Generic
  }
}
