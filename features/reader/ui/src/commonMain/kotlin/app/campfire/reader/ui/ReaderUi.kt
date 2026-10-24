// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.reader.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.outline.FormatListBulleted
import app.campfire.common.compose.icons.rounded.ArrowBack
import app.campfire.common.compose.icons.rounded.Close
import app.campfire.common.compose.icons.rounded.FormatSize
import app.campfire.common.compose.icons.rounded.History
import app.campfire.common.compose.icons.rounded.Pause
import app.campfire.common.compose.icons.rounded.PlayArrow
import app.campfire.common.compose.widgets.ErrorListState
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.common.compose.widgets.LoadingState
import app.campfire.common.compose.widgets.sheets.AdaptiveSheetOverlay
import app.campfire.common.compose.window.KeepScreenOnEffect
import app.campfire.common.compose.window.LightSystemBarsEffect
import app.campfire.common.compose.window.SystemBarsVisibleEffect
import app.campfire.common.compose.window.VolumeKeysEffect
import app.campfire.core.coroutines.LoadState
import app.campfire.core.di.UserScope
import app.campfire.core.extensions.tenthsPercent
import app.campfire.reader.api.screen.ReaderScreen
import app.campfire.reader.ui.web.BookRenderer
import app.campfire.reader.ui.web.TocEntry
import campfire.features.reader.ui.generated.resources.Res
import campfire.features.reader.ui.generated.resources.action_appearance
import campfire.features.reader.ui.generated.resources.action_back
import campfire.features.reader.ui.generated.resources.action_contents
import campfire.features.reader.ui.generated.resources.action_dismiss
import campfire.features.reader.ui.generated.resources.action_pause
import campfire.features.reader.ui.generated.resources.action_play
import campfire.features.reader.ui.generated.resources.reader_contents_title
import campfire.features.reader.ui.generated.resources.reader_end_close
import campfire.features.reader.ui.generated.resources.reader_end_finished
import campfire.features.reader.ui.generated.resources.reader_end_mark_finished
import campfire.features.reader.ui.generated.resources.reader_end_title
import campfire.features.reader.ui.generated.resources.reader_error_open
import campfire.features.reader.ui.generated.resources.reader_error_unsupported_format
import campfire.features.reader.ui.generated.resources.reader_error_unsupported_platform
import campfire.features.reader.ui.generated.resources.reader_footer
import campfire.features.reader.ui.generated.resources.reader_jump_back
import campfire.features.reader.ui.generated.resources.reader_last_page_in_chapter
import campfire.features.reader.ui.generated.resources.reader_pages_left
import campfire.features.reader.ui.generated.resources.reader_progress
import campfire.features.reader.ui.generated.resources.reader_scrubber
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.overlay.LocalOverlayHost
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@CircuitInject(ReaderScreen::class, UserScope::class)
@Composable
fun ReaderUi(
  state: ReaderUiState,
  renderer: BookRenderer,
  modifier: Modifier = Modifier,
) {
  val palette = state.theme.palette()
  // On a foldable open like a book, two pages either side of the fold
  val density = LocalDensity.current
  val layoutDirection = LocalLayoutDirection.current
  val pageInsets = WindowInsets.readerPage
  val pageWidth = LocalWindowInfo.current.containerSize.width -
    pageInsets.getLeft(density, layoutDirection) - pageInsets.getRight(density, layoutDirection)
  val foldGap = currentBookFold()?.let { foldGapPercent(it, pageWidth.toFloat(), density) }
  val style = remember(palette, state.layout, foldGap) { readerStyle(palette, state.layout, foldGap) }
  LaunchedEffect(style) {
    state.eventSink(ReaderUiEvent.StyleChanged(style))
  }

  // The system bars come and go with the reader's own bars, so the page has the screen to itself
  SystemBarsVisibleEffect(state.chromeVisible)
  LightSystemBarsEffect(light = !palette.isDark)
  KeepScreenOnEffect(keepOn = state.keepScreenOn)
  // Not while audio plays, when the keys are for its volume
  VolumeKeysEffect(
    enabled = state.volumeKeysTurnPages && state.nowPlaying?.isPlaying != true && state.appearance == null,
  ) { up ->
    state.eventSink(if (up) ReaderUiEvent.PreviousPage else ReaderUiEvent.NextPage)
    true
  }

  // The bars and panels take the page's colors
  MaterialTheme(colorScheme = MaterialTheme.colorScheme.forPage(palette)) {
    ReaderContent(state = state, renderer = renderer, modifier = modifier)
  }
}

@Composable
private fun ReaderContent(
  state: ReaderUiState,
  renderer: BookRenderer,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme

  val overlayHost = LocalOverlayHost.current
  val scope = rememberCoroutineScope()
  val showContents: () -> Unit = {
    scope.launch {
      val result = overlayHost.show(
        AdaptiveSheetOverlay<List<TocEntry>, ContentsResult>(
          model = state.toc,
          onDismiss = { ContentsResult.Dismissed },
        ) { toc, navigator ->
          ContentsList(
            toc = toc,
            currentHref = state.position.chapterHref,
            onClick = { navigator.finish(ContentsResult.Selected(it)) },
          )
        },
      )
      if (result is ContentsResult.Selected) state.eventSink(ReaderUiEvent.GoTo(result.entry))
    }
  }

  val isReading = state.content is LoadState.Loaded && state.bookError == null

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(colors.surface),
  ) {
    when (val content = state.content) {
      LoadState.Loading -> LoadingState(Modifier.fillMaxSize())
      LoadState.Error -> ErrorListState(
        message = stringResource(Res.string.reader_error_open),
        modifier = Modifier.align(Alignment.Center),
      )

      is LoadState.Loaded -> when {
        !renderer.isSupported -> ErrorListState(
          message = stringResource(Res.string.reader_error_unsupported_platform),
          modifier = Modifier.align(Alignment.Center),
        )

        content.data.format !in SupportedFormats -> ErrorListState(
          message = stringResource(Res.string.reader_error_unsupported_format, content.data.format.uppercase()),
          modifier = Modifier.align(Alignment.Center),
        )

        state.bookError != null -> ErrorListState(
          message = state.bookError,
          modifier = Modifier.align(Alignment.Center),
        )

        else -> renderer.Content(
          content = content.data,
          bridge = state.bridge,
          modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.readerPage)
            .padding(bottom = FooterHeight),
        )
      }
    }

    // While the bars are hidden: where the reader is, small, below the page
    AnimatedVisibility(
      visible = isReading && !state.chromeVisible && state.appearance == null,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .windowInsetsPadding(WindowInsets.readerPage.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)),
    ) {
      ReaderFooter(position = state.position)
    }

    AnimatedVisibility(
      visible = state.chromeVisible,
      enter = fadeIn() + slideInVertically { -it },
      exit = fadeOut() + slideOutVertically { -it },
      modifier = Modifier.align(Alignment.TopCenter),
    ) {
      ReaderTopBar(
        title = state.title,
        author = state.author,
        onBack = { state.eventSink(ReaderUiEvent.Back) },
        onContents = showContents,
        showContents = state.toc.isNotEmpty(),
        onAppearance = { state.eventSink(ReaderUiEvent.ShowAppearance) },
      )
    }

    AnimatedVisibility(
      visible = isReading && state.chromeVisible,
      enter = fadeIn() + slideInVertically { it },
      exit = fadeOut() + slideOutVertically { it },
      modifier = Modifier.align(Alignment.BottomCenter),
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
          state.jumpBack?.let { jumpBack ->
            JumpBackChip(jumpBack, onClick = { state.eventSink(ReaderUiEvent.JumpBack) })
          }
          state.nowPlaying?.let { nowPlaying ->
            NowPlayingChip(nowPlaying, onClick = { state.eventSink(ReaderUiEvent.PlayPause) })
          }
        }
        ReaderBottomBar(
          position = state.position,
          toc = state.toc,
          onScrub = { state.eventSink(ReaderUiEvent.Scrub(it)) },
        )
      }
    }

    AnimatedVisibility(
      visible = isReading && !state.chromeVisible && state.appearance == null && state.endOfBook != null,
      enter = fadeIn() + slideInVertically { it / 2 },
      exit = fadeOut() + slideOutVertically { it / 2 },
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .windowInsetsPadding(WindowInsets.readerPage.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
        .padding(bottom = FooterHeight + 8.dp),
    ) {
      EndOfBookCard(
        isFinished = state.endOfBook?.isFinished == true,
        onMarkFinished = { state.eventSink(ReaderUiEvent.MarkFinished) },
        onClose = { state.eventSink(ReaderUiEvent.Back) },
        onDismiss = { state.eventSink(ReaderUiEvent.DismissEndOfBook) },
      )
    }

    AnimatedVisibility(
      visible = isReading && state.appearance != null,
      enter = fadeIn() + slideInVertically { it },
      exit = fadeOut() + slideOutVertically { it },
      modifier = Modifier.align(Alignment.BottomCenter),
    ) {
      // Kept through the exit animation, after the panel's state is gone
      var shown by remember { mutableStateOf(state.appearance) }
      state.appearance?.let { shown = it }
      shown?.let { panel ->
        AppearancePanel(
          panel = panel,
          theme = state.theme,
          keepScreenOn = state.keepScreenOn,
          volumeKeysTurnPages = state.volumeKeysTurnPages,
          eventSink = state.eventSink,
        )
      }
    }
  }
}

private sealed interface ContentsResult {
  data object Dismissed : ContentsResult
  data class Selected(val entry: TocEntry) : ContentsResult
}

@Composable
private fun ReaderFooter(
  position: ReaderPosition,
  modifier: Modifier = Modifier,
) {
  val percent = position.progress.tenthsPercent
  val text = when (val pagesLeft = position.pagesLeftInChapter) {
    null -> stringResource(Res.string.reader_progress, percent)
    else -> stringResource(Res.string.reader_footer, pagesLeftLabel(pagesLeft), percent)
  }
  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
      .fillMaxWidth()
      .height(FooterHeight),
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 1,
    )
  }
}

@Composable
private fun pagesLeftLabel(pagesLeft: Int): String = if (pagesLeft == 0) {
  stringResource(Res.string.reader_last_page_in_chapter)
} else {
  pluralStringResource(Res.plurals.reader_pages_left, pagesLeft, pagesLeft)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderTopBar(
  title: String,
  author: String?,
  onBack: () -> Unit,
  onContents: () -> Unit,
  showContents: Boolean,
  onAppearance: () -> Unit,
  modifier: Modifier = Modifier,
) {
  TopAppBar(
    title = {
      Column {
        Text(
          text = title,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        if (author != null) {
          Text(
            text = author,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    },
    navigationIcon = {
      val backLabel = stringResource(Res.string.action_back)
      IconButtonTooltip(text = backLabel) {
        IconButton(onClick = onBack) {
          Icon(CampfireIcons.Rounded.ArrowBack, contentDescription = backLabel)
        }
      }
    },
    actions = {
      if (showContents) {
        val contentsLabel = stringResource(Res.string.action_contents)
        IconButtonTooltip(text = contentsLabel) {
          IconButton(onClick = onContents) {
            Icon(CampfireIcons.Outline.FormatListBulleted, contentDescription = contentsLabel)
          }
        }
      }
      val appearanceLabel = stringResource(Res.string.action_appearance)
      IconButtonTooltip(text = appearanceLabel) {
        IconButton(onClick = onAppearance) {
          Icon(CampfireIcons.Rounded.FormatSize, contentDescription = appearanceLabel)
        }
      }
    },
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ),
    modifier = modifier,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderBottomBar(
  position: ReaderPosition,
  toc: List<TocEntry>,
  onScrub: (Float) -> Unit,
  modifier: Modifier = Modifier,
) {
  // Where a drag is, until it's let go: the book only moves then
  var dragging by remember { mutableStateOf<Float?>(null) }
  val shown = dragging ?: position.progress
  val chapterStarts = remember(toc) {
    toc.filter { it.depth == 0 }.mapNotNull { it.fraction }.filter { it > 0f && it < 1f }
  }

  Surface(
    color = MaterialTheme.colorScheme.surfaceContainer,
    modifier = modifier.fillMaxWidth(),
  ) {
    Column(
      verticalArrangement = Arrangement.spacedBy(4.dp),
      modifier = Modifier
        .navigationBarsPadding()
        .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        // The part over its chapter, each wrapping rather than cut off
        Column(
          modifier = Modifier
            .weight(1f)
            .alignByBaseline(),
        ) {
          position.part?.let { part ->
            Text(
              text = part,
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = LabelMaxLines,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Text(
            text = position.chapter.orEmpty(),
            style = MaterialTheme.typography.labelLarge,
            maxLines = LabelMaxLines,
            overflow = TextOverflow.Ellipsis,
          )
        }
        position.pagesLeftInChapter?.let { pagesLeft ->
          Spacer(Modifier.padding(horizontal = 4.dp))
          Text(
            text = pagesLeftLabel(pagesLeft),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.alignByBaseline(),
          )
        }
      }
      Row(verticalAlignment = Alignment.CenterVertically) {
        val scrubberLabel = stringResource(Res.string.reader_scrubber)
        val tickColor = MaterialTheme.colorScheme.onSurfaceVariant
        Slider(
          value = shown,
          onValueChange = { dragging = it },
          onValueChangeFinished = {
            dragging?.let(onScrub)
            dragging = null
          },
          track = { sliderState ->
            Box {
              SliderDefaults.Track(sliderState = sliderState)
              // Where each chapter starts
              Canvas(Modifier.matchParentSize()) {
                chapterStarts.forEach { fraction ->
                  drawCircle(
                    color = tickColor,
                    radius = 1.5.dp.toPx(),
                    center = Offset(size.width * fraction, size.height / 2),
                  )
                }
              }
            }
          },
          modifier = Modifier
            .weight(1f)
            .semantics { contentDescription = scrubberLabel },
        )
        Spacer(Modifier.padding(horizontal = 6.dp))
        Text(
          text = stringResource(Res.string.reader_progress, shown.tenthsPercent),
          style = MaterialTheme.typography.labelLarge,
          modifier = Modifier.widthIn(min = 48.dp),
        )
      }
    }
  }
}

@Composable
private fun JumpBackChip(
  jumpBack: JumpBack,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  AssistChip(
    onClick = onClick,
    label = { Text(stringResource(Res.string.reader_jump_back, (jumpBack.progress * 100).roundToInt())) },
    leadingIcon = {
      Icon(CampfireIcons.Rounded.History, contentDescription = null, modifier = Modifier.padding(2.dp))
    },
    colors = AssistChipDefaults.assistChipColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ),
    border = null,
    modifier = modifier,
  )
}

@Composable
private fun NowPlayingChip(
  nowPlaying: NowPlaying,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  AssistChip(
    onClick = onClick,
    label = {
      Text(
        text = nowPlaying.title,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.widthIn(max = 180.dp),
      )
    },
    leadingIcon = {
      Icon(
        imageVector = if (nowPlaying.isPlaying) CampfireIcons.Rounded.Pause else CampfireIcons.Rounded.PlayArrow,
        contentDescription = stringResource(
          if (nowPlaying.isPlaying) Res.string.action_pause else Res.string.action_play,
        ),
      )
    },
    colors = AssistChipDefaults.assistChipColors(
      containerColor = MaterialTheme.colorScheme.secondaryContainer,
      labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
      leadingIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ),
    border = null,
    modifier = modifier,
  )
}

@Composable
private fun EndOfBookCard(
  isFinished: Boolean,
  onMarkFinished: () -> Unit,
  onClose: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  ElevatedCard(
    modifier = modifier
      .padding(horizontal = 16.dp)
      .widthIn(max = 480.dp)
      .fillMaxWidth(),
  ) {
    Column(
      verticalArrangement = Arrangement.spacedBy(12.dp),
      modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 16.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = stringResource(Res.string.reader_end_title),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.weight(1f),
        )
        val dismissLabel = stringResource(Res.string.action_dismiss)
        IconButtonTooltip(text = dismissLabel) {
          IconButton(onClick = onDismiss) {
            Icon(CampfireIcons.Rounded.Close, contentDescription = dismissLabel)
          }
        }
      }
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
      ) {
        TextButton(onClick = onClose) {
          Text(stringResource(Res.string.reader_end_close))
        }
        Button(onClick = onMarkFinished, enabled = !isFinished) {
          Text(
            stringResource(if (isFinished) Res.string.reader_end_finished else Res.string.reader_end_mark_finished),
          )
        }
      }
    }
  }
}

@Composable
private fun ContentsList(
  toc: List<TocEntry>,
  currentHref: String?,
  onClick: (TocEntry) -> Unit,
  modifier: Modifier = Modifier,
) {
  val current = toc.indexOfFirst { it.href == currentHref }
  val listState = rememberLazyListState(initialFirstVisibleItemIndex = (current - 2).coerceAtLeast(0))
  Column(modifier = modifier) {
    Text(
      text = stringResource(Res.string.reader_contents_title),
      style = MaterialTheme.typography.titleLarge,
      modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
    )
    LazyColumn(state = listState) {
      items(toc) { entry ->
        val isCurrent = entry.href == currentHref
        Text(
          text = entry.label,
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = if (isCurrent) FontWeight.Bold else null,
          color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.Unspecified,
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(entry) }
            .padding(start = (24 + entry.depth * 16).dp, end = 24.dp, top = 12.dp, bottom = 12.dp),
        )
      }
    }
  }
}

private val FooterHeight = 28.dp
private const val LabelMaxLines = 2

private val SupportedFormats = setOf("epub", "cbz", "mobi", "azw3", "fb2")
