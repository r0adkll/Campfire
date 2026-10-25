// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.management.ui.match

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.extensions.readoutFormat
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Check
import app.campfire.common.compose.icons.rounded.Search
import app.campfire.common.compose.widgets.CoverImage
import app.campfire.common.compose.widgets.coverImageShape
import app.campfire.common.compose.widgets.placeholderBookPainter
import app.campfire.common.compose.widgets.sheets.AdaptiveSheetOverlay
import app.campfire.management.api.match.BookMatch
import campfire.features.management.ui.generated.resources.Res
import campfire.features.management.ui.generated.resources.action_retry
import campfire.features.management.ui.generated.resources.action_search
import campfire.features.management.ui.generated.resources.cd_match_cover
import campfire.features.management.ui.generated.resources.match_author
import campfire.features.management.ui.generated.resources.match_book_length
import campfire.features.management.ui.generated.resources.match_confidence
import campfire.features.management.ui.generated.resources.match_empty
import campfire.features.management.ui.generated.resources.match_failed
import campfire.features.management.ui.generated.resources.match_results
import campfire.features.management.ui.generated.resources.match_same_asin
import campfire.features.management.ui.generated.resources.match_same_length
import campfire.features.management.ui.generated.resources.match_source
import campfire.features.management.ui.generated.resources.match_sources_failed
import campfire.features.management.ui.generated.resources.match_title
import com.slack.circuit.overlay.LocalOverlayHost
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.flow.filter
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Matching the book against a source: a search and its results and, once one is picked, the
 * review of what it would change, in a sheet over them.
 */
@Composable
internal fun MatchSection(
  state: MatchUiState,
  enabled: Boolean,
  modifier: Modifier = Modifier,
) {
  MatchSearchPane(
    state = state,
    enabled = enabled,
    modifier = modifier.fillMaxSize(),
  )
  ReviewSheet(state = state, enabled = enabled)
}

/**
 * Shows the review in a sheet while there is one: at the bottom where the window has the height,
 * at the side where it doesn't. Applying closes it from the presenter's side; back, the scrim or a
 * swipe close it from here.
 */
@Composable
private fun ReviewSheet(
  state: MatchUiState,
  enabled: Boolean,
) {
  val overlayHost = LocalOverlayHost.current
  val latestState by rememberUpdatedState(state)
  val latestEnabled by rememberUpdatedState(enabled)
  LaunchedEffect(overlayHost) {
    snapshotFlow { latestState.review != null }
      .filter { it }
      .collect {
        overlayHost.show(
          AdaptiveSheetOverlay(model = Unit, onDismiss = {}, skipPartiallyExpanded = true) { _, navigator ->
            val review = latestState.review
            // The review shown last stays while the sheet slides away
            val shown = remember { arrayOfNulls<MatchReviewState>(1) }
            if (review != null) shown[0] = review
            LaunchedEffect(review == null) {
              if (review == null) navigator.finish(Unit)
            }
            shown[0]?.let { MatchReviewSheet(review = it, enabled = latestEnabled, eventSink = latestState.eventSink) }
          },
        )
        latestState.eventSink(MatchUiEvent.CloseReview)
      }
  }
}

@Composable
private fun MatchSearchPane(
  state: MatchUiState,
  enabled: Boolean,
  modifier: Modifier = Modifier,
) {
  val search = state.search
  // The search scrolls away with the results, which a short window, like a phone on its side,
  // has little room for under the top bar
  LazyColumn(modifier = modifier.imePadding().testTag("match_results")) {
    item(key = "search") {
      SearchHeader(
        state = state,
        enabled = enabled,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
      )
    }
    item(key = "progress") {
      Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        if (search == MatchSearch.Searching) {
          LinearProgressIndicator(Modifier.fillMaxWidth().testTag("match_searching"))
        }
      }
    }

    when (search) {
      is MatchSearch.Found -> if (search.matches.isEmpty()) {
        item(key = "empty") {
          Notice(text = stringResource(Res.string.match_empty, search.provider.name))
        }
      } else {
        item(key = "count") {
          ResultsHeader(
            count = search.matches.size,
            bookDuration = state.bookDuration,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
          )
        }
        items(search.matches) { match ->
          MatchCandidate(
            match = match,
            bookDuration = state.bookDuration,
            bookAsin = state.bookAsin,
            selected = match == state.review?.match,
            onClick = { state.eventSink(MatchUiEvent.Review(match)) },
          )
        }
      }

      is MatchSearch.Failed -> item(key = "failed") {
        Notice(
          text = search.provider?.let { stringResource(Res.string.match_failed, it.name) }
            ?: stringResource(Res.string.match_sources_failed),
          onRetry = { state.eventSink(MatchUiEvent.Search) },
        )
      }

      MatchSearch.NotSearched, MatchSearch.Searching -> Unit
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchHeader(
  state: MatchUiState,
  enabled: Boolean,
  modifier: Modifier = Modifier,
) {
  val search = { state.eventSink(MatchUiEvent.Search) }
  Column(
    verticalArrangement = Arrangement.spacedBy(8.dp),
    modifier = modifier,
  ) {
    var choosing by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
      expanded = choosing,
      onExpandedChange = { choosing = it && state.providers.isNotEmpty() },
    ) {
      OutlinedTextField(
        value = state.provider?.name.orEmpty(),
        onValueChange = {},
        readOnly = true,
        enabled = enabled,
        singleLine = true,
        label = { Text(stringResource(Res.string.match_source)) },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = choosing) },
        modifier = Modifier
          .fillMaxWidth()
          .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled)
          .testTag("match_source"),
      )
      ExposedDropdownMenu(
        expanded = choosing,
        onDismissRequest = { choosing = false },
      ) {
        state.providers.forEach { provider ->
          DropdownMenuItem(
            text = { Text(provider.name) },
            onClick = {
              choosing = false
              state.eventSink(MatchUiEvent.ChooseProvider(provider))
            },
            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
          )
        }
      }
    }
    OutlinedTextField(
      state = state.title,
      enabled = enabled,
      label = { Text(stringResource(Res.string.match_title)) },
      lineLimits = TextFieldLineLimits.SingleLine,
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
      onKeyboardAction = { search() },
      modifier = Modifier.fillMaxWidth().testTag("match_title"),
    )
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      OutlinedTextField(
        state = state.author,
        enabled = enabled,
        label = { Text(stringResource(Res.string.match_author)) },
        lineLimits = TextFieldLineLimits.SingleLine,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        onKeyboardAction = { search() },
        modifier = Modifier.weight(1f).testTag("match_author"),
      )
      Button(
        onClick = search,
        enabled = enabled && state.search != MatchSearch.Searching,
        modifier = Modifier.padding(top = 8.dp).testTag("match_search"),
      ) {
        Icon(CampfireIcons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(stringResource(Res.string.action_search), modifier = Modifier.padding(start = 8.dp))
      }
    }
  }
}

@Composable
private fun ResultsHeader(
  count: Int,
  bookDuration: Duration?,
  modifier: Modifier = Modifier,
) {
  val results = pluralStringResource(Res.plurals.match_results, count, count)
  Text(
    text = if (bookDuration != null) {
      "$results · ${stringResource(Res.string.match_book_length, bookDuration.readoutFormat().trim())}"
    } else {
      results
    },
    style = MaterialTheme.typography.labelLarge,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = modifier,
  )
}

/** A result, with what tells it apart: its length beside the book's and how well it matches */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MatchCandidate(
  match: BookMatch,
  bookDuration: Duration?,
  bookAsin: String?,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
    modifier = modifier
      .fillMaxWidth()
      .clickable(role = Role.Button, onClick = onClick)
      .testTag("match_candidate"),
  ) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(16.dp),
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
      CoverImage(
        imageUrl = match.coverUrl,
        contentDescription = stringResource(Res.string.cd_match_cover, match.title.orEmpty()),
        size = CandidateCoverSize,
        placeholder = placeholderBookPainter(),
        shape = coverImageShape(CandidateCoverSize),
        modifier = Modifier.size(CandidateCoverSize),
      )
      Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.weight(1f),
      ) {
        Text(
          text = match.title.orEmpty(),
          style = MaterialTheme.typography.titleMedium,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        val byline = listOfNotNull(
          match.authors.joinToString().takeIf { it.isNotEmpty() },
          match.narrators.joinToString().takeIf { it.isNotEmpty() },
          match.publishedYear,
        ).joinToString(" · ")
        if (byline.isNotEmpty()) {
          Text(
            text = byline,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
          modifier = Modifier.padding(top = 4.dp),
        ) {
          match.series.forEach { series ->
            Tag(text = series.sequence?.let { "${series.name} #$it" } ?: series.name)
          }
          LengthTag(bookDuration = bookDuration, matchDuration = match.duration)
          match.confidence?.let { confidence ->
            Tag(text = stringResource(Res.string.match_confidence, (confidence * 100).roundToInt()))
          }
          if (bookAsin != null && match.asin.equals(bookAsin, ignoreCase = true)) {
            Tag(text = stringResource(Res.string.match_same_asin), emphasized = true)
          }
        }
      }
    }
  }
}

/** How the result's length compares with the book's, when both are known */
@Composable
private fun LengthTag(
  bookDuration: Duration?,
  matchDuration: Duration?,
  modifier: Modifier = Modifier,
) {
  if (bookDuration == null || matchDuration == null) return
  val difference = matchDuration - bookDuration
  if (abs(difference.inWholeSeconds) <= SameLengthWithin.inWholeSeconds) {
    Tag(
      text = stringResource(Res.string.match_same_length),
      emphasized = true,
      leadingCheck = true,
      modifier = modifier,
    )
  } else {
    val sign = if (difference.isPositive()) "+" else "−"
    Tag(text = sign + difference.absoluteValue.readoutFormat().trim(), modifier = modifier)
  }
}

@Composable
private fun Tag(
  text: String,
  modifier: Modifier = Modifier,
  emphasized: Boolean = false,
  leadingCheck: Boolean = false,
) {
  val colors = MaterialTheme.colorScheme
  Surface(
    shape = MaterialTheme.shapes.small,
    color = if (emphasized) colors.primaryContainer else colors.surfaceContainerHigh,
    contentColor = if (emphasized) colors.onPrimaryContainer else colors.onSurfaceVariant,
    modifier = modifier,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
      if (leadingCheck) {
        Icon(CampfireIcons.Rounded.Check, contentDescription = null, modifier = Modifier.size(14.dp))
      }
      Text(text = text, style = MaterialTheme.typography.labelMedium)
    }
  }
}

@Composable
private fun Notice(
  text: String,
  modifier: Modifier = Modifier,
  onRetry: (() -> Unit)? = null,
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(8.dp),
    modifier = modifier
      .fillMaxWidth()
      .padding(24.dp),
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (onRetry != null) {
      TextButton(onClick = onRetry, modifier = Modifier.testTag("match_retry")) {
        Text(stringResource(Res.string.action_retry))
      }
    }
  }
}

private val SearchPaneWidth = 400.dp
private val CandidateCoverSize = 64.dp

/** A source's length within this of the book's is as good as the same */
private val SameLengthWithin = 2.minutes
