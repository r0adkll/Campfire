// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.reader.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.AudioPlayerHolder
import app.campfire.core.coroutines.LoadState
import app.campfire.core.coroutines.map
import app.campfire.core.di.UserScope
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.reader.api.Ebook
import app.campfire.reader.api.EbookContentsEntry
import app.campfire.reader.api.EbookContentsRepository
import app.campfire.reader.api.EbookRepository
import app.campfire.reader.api.ReaderBookSettingsRepository
import app.campfire.reader.api.screen.ReaderScreen
import app.campfire.reader.ui.web.ReaderBridge
import app.campfire.reader.ui.web.ReaderContent
import app.campfire.reader.ui.web.ReaderMessage
import app.campfire.reader.ui.web.ReaderStyle
import app.campfire.reader.ui.web.TocEntry
import app.campfire.settings.api.ReaderLayout
import app.campfire.settings.api.ReaderLayoutOverrides
import app.campfire.settings.api.ReaderSettings
import app.campfire.settings.api.ReaderTheme
import app.campfire.user.api.MediaProgressRepository
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@CircuitInject(ReaderScreen::class, UserScope::class)
@Inject
class ReaderPresenter(
  private val screen: ReaderScreen,
  private val navigator: Navigator,
  private val repository: EbookRepository,
  private val mediaProgressRepository: MediaProgressRepository,
  private val audioPlayerHolder: AudioPlayerHolder,
  private val readerSettings: ReaderSettings,
  private val bookSettings: ReaderBookSettingsRepository,
  private val contentsRepository: EbookContentsRepository,
) : Presenter<ReaderUiState> {

  @Composable
  override fun present(): ReaderUiState {
    val scope = rememberCoroutineScope()
    val bridge = remember { ReaderBridge() }

    val ebook by produceState<LoadState<out Ebook>>(LoadState.Loading) {
      value = repository.open(screen.libraryItemId).fold(
        onSuccess = { LoadState.Loaded(it) },
        onFailure = {
          bark(LogPriority.ERROR, throwable = it) { "Couldn't open ebook ${screen.libraryItemId}" }
          LoadState.Error
        },
      )
    }
    val content = remember(ebook) { ebook.map { ReaderContent(it) } }

    var style by remember { mutableStateOf<ReaderStyle?>(null) }
    var chromeVisible by remember { mutableStateOf(false) }
    var position by remember { mutableStateOf(ReaderPosition()) }
    var currentLocation by remember { mutableStateOf<String?>(null) }
    var atEnd by remember { mutableStateOf(false) }
    var endDismissed by remember { mutableStateOf(false) }
    var jumpBack by remember { mutableStateOf<JumpBack?>(null) }
    var toc by remember { mutableStateOf(emptyList<TocEntry>()) }
    var bookError by remember { mutableStateOf<String?>(null) }
    val pendingSave = remember { MutableStateFlow<ReaderMessage.Relocate?>(null) }

    val theme by remember { readerSettings.observeTheme() }.collectAsState()
    val globalLayout by remember { readerSettings.observeLayout() }.collectAsState()
    val overrides by remember {
      bookSettings.observeOverrides(screen.libraryItemId)
    }.collectAsState(ReaderLayoutOverrides.None)
    val keepScreenOn by remember { readerSettings.observeKeepScreenOn() }.collectAsState()
    val volumeKeys by remember { readerSettings.observeVolumeKeysTurnPages() }.collectAsState()
    // The appearance panel's scope while it's open
    var appearanceScope by remember { mutableStateOf<AppearanceScope?>(null) }

    val isFinished by remember {
      mediaProgressRepository.observeProgress(screen.libraryItemId).map { it?.isFinished == true }
    }.collectAsState(false)

    // Audio keeps playing while reading; a chip offers it once it's playing, and stays after it's
    // paused from there so it can be resumed
    var pausedHere by remember { mutableStateOf(false) }
    val audio by remember { audioPlayerHolder.observeNowPlaying() }.collectAsState(null)
    val nowPlaying = audio?.takeIf { it.isPlaying || pausedHere }

    LaunchedEffect(ebook) {
      val opened = (ebook as? LoadState.Loaded)?.data ?: return@LaunchedEffect
      position = position.copy(progress = opened.progress ?: 0f)
      // The page is ready again after its renderer is replaced, and reopens where the reader is
      var location = opened.location
      // Opened at an entry in the contents, it goes there once, after the place it was left is
      // laid out, and offers the way back to it
      var href = screen.href
      for (message in bridge.messages) {
        when (message) {
          ReaderMessage.Ready -> bridge.open(
            url = ReaderContent(opened).bookPath,
            format = opened.format,
            location = location,
            style = style ?: DefaultStyle,
          )

          is ReaderMessage.Opened -> {
            toc = message.toc
            // The book's details show its contents, which only the file knows
            launch { contentsRepository.saveContents(screen.libraryItemId, message.toc.map { it.asContentsEntry() }) }
          }

          is ReaderMessage.Restored -> {
            bark { "Restored ${opened.location}: ${message.restored}" }
            href?.let { target ->
              href = null
              location?.let { jumpBack = JumpBack(it, opened.progress ?: 0f) }
              chromeVisible = jumpBack != null
              bridge.goTo(target)
            }
          }

          is ReaderMessage.Relocate -> {
            position = ReaderPosition(
              chapter = message.chapter,
              chapterHref = message.chapterHref,
              progress = message.progress ?: position.progress,
              pagesLeftInChapter = message.pagesLeft,
            )
            currentLocation = message.location ?: currentLocation
            if (!message.atEnd) endDismissed = false
            atEnd = message.atEnd
            if (message.save && message.location != null) {
              location = message.location
              pendingSave.value = message
            }
          }

          ReaderMessage.Tap -> if (appearanceScope != null) {
            appearanceScope = null
          } else {
            chromeVisible = !chromeVisible
            if (!chromeVisible) jumpBack = null
          }
          is ReaderMessage.Error -> {
            bark(if (message.fatal) LogPriority.ERROR else LogPriority.WARN) { "Reader error: ${message.message}" }
            if (message.fatal) bookError = message.message
          }
        }
      }
    }

    // Every page saves here at once (the server upload waits for the reader to settle), so closing
    // the book straight after turning a page keeps it. The state flow drops pages turned while a
    // save is still writing; only the latest matters.
    LaunchedEffect(Unit) {
      pendingSave
        .filterNotNull()
        .collect { relocate ->
          val location = relocate.location ?: return@collect
          repository.saveProgress(screen.libraryItemId, location, relocate.progress ?: 0f)
            .onSuccess { bark { "Saved ebook location $location (${relocate.progress})" } }
            .onFailure { bark(LogPriority.WARN, throwable = it) { "Couldn't save ebook location" } }
        }
    }

    // Remember where the reader was before a jump, once: jumping again keeps the first place
    fun rememberJumpOrigin() {
      val from = currentLocation ?: return
      if (jumpBack == null) jumpBack = JumpBack(from, position.progress)
    }

    val opened = (ebook as? LoadState.Loaded)?.data
    return ReaderUiState(
      title = opened?.title.orEmpty(),
      author = opened?.author,
      content = content,
      bridge = bridge,
      chromeVisible = chromeVisible,
      position = position,
      toc = toc,
      jumpBack = jumpBack,
      nowPlaying = nowPlaying,
      endOfBook = EndOfBook(isFinished).takeIf { atEnd && !endDismissed },
      theme = theme,
      // While the panel edits all books, the page shows them; otherwise this book as it reads
      layout = if (appearanceScope == AppearanceScope.AllBooks) globalLayout else overrides.applyTo(globalLayout),
      keepScreenOn = keepScreenOn,
      volumeKeysTurnPages = volumeKeys,
      appearance = appearanceScope?.let { scope ->
        AppearancePanel(
          scope = scope,
          layout = if (scope == AppearanceScope.AllBooks) globalLayout else overrides.applyTo(globalLayout),
          overrides = overrides,
        )
      },
      bookError = bookError,
    ) { event ->
      when (event) {
        ReaderUiEvent.Back -> navigator.pop()
        ReaderUiEvent.ToggleChrome -> {
          chromeVisible = !chromeVisible
          if (!chromeVisible) jumpBack = null
        }

        is ReaderUiEvent.GoTo -> {
          rememberJumpOrigin()
          bridge.goTo(event.entry.href)
        }

        is ReaderUiEvent.Scrub -> {
          rememberJumpOrigin()
          bridge.goToFraction(event.fraction.coerceIn(0f, 1f))
        }

        ReaderUiEvent.JumpBack -> jumpBack?.let {
          bridge.goToLocation(it.location)
          jumpBack = null
        }

        ReaderUiEvent.PlayPause -> audioPlayerHolder.currentPlayer.value?.let { player ->
          pausedHere = player.state.value == AudioPlayer.State.Playing
          player.playPause()
        }

        ReaderUiEvent.MarkFinished -> scope.launch {
          mediaProgressRepository.markFinished(screen.libraryItemId)
        }

        ReaderUiEvent.DismissEndOfBook -> endDismissed = true

        is ReaderUiEvent.StyleChanged -> {
          if (style != null) bridge.setStyle(event.style)
          style = event.style
        }

        ReaderUiEvent.NextPage -> bridge.next()
        ReaderUiEvent.PreviousPage -> bridge.prev()

        ReaderUiEvent.ShowAppearance -> {
          chromeVisible = false
          jumpBack = null
          appearanceScope = AppearanceScope.AllBooks
        }
        ReaderUiEvent.HideAppearance -> appearanceScope = null
        is ReaderUiEvent.AppearanceScopeChanged -> appearanceScope = event.scope
        is ReaderUiEvent.ThemeChanged -> readerSettings.theme = event.theme
        is ReaderUiEvent.LayoutChanged -> when (appearanceScope) {
          AppearanceScope.ThisBook -> scope.launch {
            bookSettings.setOverrides(screen.libraryItemId, event.change.applyTo(overrides))
          }
          else -> readerSettings.layout = event.change.applyTo(globalLayout)
        }
        ReaderUiEvent.ResetToDefaults -> {
          readerSettings.theme = ReaderTheme.FollowApp
          readerSettings.layout = ReaderLayout()
        }
        ReaderUiEvent.ResetBookLayout -> scope.launch {
          bookSettings.setOverrides(screen.libraryItemId, ReaderLayoutOverrides.None)
        }
        is ReaderUiEvent.KeepScreenOnChanged -> readerSettings.keepScreenOn = event.keepOn
        is ReaderUiEvent.VolumeKeysChanged -> readerSettings.volumeKeysTurnPages = event.enabled
      }
    }
  }
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun AudioPlayerHolder.observeNowPlaying(): Flow<NowPlaying?> =
  currentPlayer.flatMapLatest { player ->
    player ?: return@flatMapLatest flowOf(null)
    combine(player.state, player.currentMetadata) { state, metadata ->
      val title = player.preparedSession?.libraryItem?.media?.metadata?.title ?: metadata.title
      when (state) {
        AudioPlayer.State.Playing, AudioPlayer.State.Buffering, AudioPlayer.State.Paused ->
          title?.let { NowPlaying(it, isPlaying = state != AudioPlayer.State.Paused) }
        else -> null
      }
    }
  }

private val DefaultStyle = ReaderStyle(
  background = "#ffffff",
  foreground = "#000000",
  link = "#1a73e8",
)

private fun TocEntry.asContentsEntry() = EbookContentsEntry(
  label = label,
  href = href,
  depth = depth,
  fraction = fraction,
)
