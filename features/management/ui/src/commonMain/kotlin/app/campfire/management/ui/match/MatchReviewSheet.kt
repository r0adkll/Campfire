// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.management.ui.match

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.ArrowForward
import app.campfire.common.compose.icons.rounded.KeyboardArrowDown
import app.campfire.common.compose.widgets.CoverImage
import app.campfire.common.compose.widgets.coverImageShape
import app.campfire.common.compose.widgets.placeholderBookPainter
import app.campfire.common.compose.widgets.sheets.LocalSheetPresentation
import app.campfire.common.compose.widgets.sheets.SheetPresentation
import app.campfire.management.api.match.ImageSize
import campfire.features.management.ui.generated.resources.Res
import campfire.features.management.ui.generated.resources.cd_current_cover
import campfire.features.management.ui.generated.resources.cd_new_cover
import campfire.features.management.ui.generated.resources.label_abridged
import campfire.features.management.ui.generated.resources.label_asin
import campfire.features.management.ui.generated.resources.label_authors
import campfire.features.management.ui.generated.resources.label_cover
import campfire.features.management.ui.generated.resources.label_description
import campfire.features.management.ui.generated.resources.label_explicit
import campfire.features.management.ui.generated.resources.label_genres
import campfire.features.management.ui.generated.resources.label_isbn
import campfire.features.management.ui.generated.resources.label_language
import campfire.features.management.ui.generated.resources.label_narrators
import campfire.features.management.ui.generated.resources.label_published_year
import campfire.features.management.ui.generated.resources.label_publisher
import campfire.features.management.ui.generated.resources.label_series_name
import campfire.features.management.ui.generated.resources.label_subtitle
import campfire.features.management.ui.generated.resources.label_tags
import campfire.features.management.ui.generated.resources.label_title
import campfire.features.management.ui.generated.resources.review_add
import campfire.features.management.ui.generated.resources.review_all
import campfire.features.management.ui.generated.resources.review_apply_form
import campfire.features.management.ui.generated.resources.review_apply_save
import campfire.features.management.ui.generated.resources.review_cover_larger
import campfire.features.management.ui.generated.resources.review_cover_needs_upload
import campfire.features.management.ui.generated.resources.review_cover_no_undo
import campfire.features.management.ui.generated.resources.review_cover_none
import campfire.features.management.ui.generated.resources.review_cover_size
import campfire.features.management.ui.generated.resources.review_cover_smaller
import campfire.features.management.ui.generated.resources.review_current_cover
import campfire.features.management.ui.generated.resources.review_currently
import campfire.features.management.ui.generated.resources.review_currently_none
import campfire.features.management.ui.generated.resources.review_empty_only
import campfire.features.management.ui.generated.resources.review_from
import campfire.features.management.ui.generated.resources.review_from_confidence
import campfire.features.management.ui.generated.resources.review_new_cover
import campfire.features.management.ui.generated.resources.review_none
import campfire.features.management.ui.generated.resources.review_replace
import campfire.features.management.ui.generated.resources.review_selected
import campfire.features.management.ui.generated.resources.review_title
import campfire.features.management.ui.generated.resources.review_unchanged
import campfire.features.management.ui.generated.resources.review_use_new_cover
import campfire.features.management.ui.generated.resources.value_no
import campfire.features.management.ui.generated.resources.value_yes
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * A result's details beside the book's, each to take or leave, and the ways to apply those taken,
 * as the content of the sheet the review opens in over the results.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MatchReviewSheet(
  review: MatchReviewState,
  enabled: Boolean,
  eventSink: (MatchUiEvent) -> Unit,
  modifier: Modifier = Modifier,
) {
  var showUnchanged by remember(review.match) { mutableStateOf(false) }
  val canApply = enabled && review.selected.isNotEmpty()
  // A side sheet is as tall as its region, so its buttons go to the foot; a bottom sheet is only
  // as tall as what's in it, so they follow the details
  val fillHeight = LocalSheetPresentation.current == SheetPresentation.Side

  Column(modifier = modifier.testTag("match_review")) {
    LazyColumn(modifier = Modifier.weight(1f, fill = fillHeight).testTag("review_rows")) {
      item(key = "header") {
        ReviewHeader(
          review = review,
          modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp),
        )
      }
      item(key = "modes") {
        SelectionModes(
          review = review,
          enabled = enabled,
          onSelect = { eventSink(MatchUiEvent.Select(it)) },
          modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
        )
      }
      review.cover?.let { cover ->
        item(key = "cover") {
          CoverChange(
            row = cover,
            currentSize = review.currentCoverSize,
            newSize = review.newCoverSize,
            checked = MatchField.Cover in review.selected,
            enabled = enabled && review.canChangeCover,
            canChangeCover = review.canChangeCover,
            onToggle = { eventSink(MatchUiEvent.Toggle(MatchField.Cover)) },
            // A side sheet is for a short window, with less height to give the covers
            maxCoverSize = if (fillHeight) SideSheetCoverSize else CoverHeroSize,
          )
        }
      }
      items(review.details, key = { it.field }) { row ->
        ReviewRow(
          row = row,
          checked = row.field in review.selected,
          enabled = enabled,
          merge = review.merges[row.field] ?: ListMerge.Replace,
          onToggle = { eventSink(MatchUiEvent.Toggle(row.field)) },
          onMerge = { eventSink(MatchUiEvent.Merge(row.field, it)) },
        )
      }
      if (review.unchanged.isNotEmpty()) {
        item(key = "unchanged") {
          TextButton(
            onClick = { showUnchanged = !showUnchanged },
            modifier = Modifier.padding(horizontal = 16.dp).testTag("review_unchanged"),
          ) {
            Icon(
              imageVector = CampfireIcons.Rounded.KeyboardArrowDown,
              contentDescription = null,
              modifier = Modifier.rotate(if (showUnchanged) 180f else 0f),
            )
            Text(
              text = pluralStringResource(Res.plurals.review_unchanged, review.unchanged.size, review.unchanged.size),
              modifier = Modifier.padding(start = 8.dp),
            )
          }
        }
      }
      if (showUnchanged) {
        items(review.unchanged, key = { "same-${it.field}" }) { row ->
          ReviewRow(
            row = row,
            checked = false,
            enabled = false,
            merge = ListMerge.Replace,
            onToggle = {},
            onMerge = {},
          )
        }
      }
    }

    HorizontalDivider()
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
    ) {
      OutlinedButton(
        onClick = { eventSink(MatchUiEvent.ApplyToForm) },
        enabled = canApply,
        modifier = Modifier.testTag("review_apply_form"),
      ) {
        Text(stringResource(Res.string.review_apply_form))
      }
      Button(
        onClick = { eventSink(MatchUiEvent.ApplyAndSave) },
        enabled = canApply,
        modifier = Modifier.testTag("review_apply_save"),
      ) {
        Text(pluralStringResource(Res.plurals.review_apply_save, review.selected.size, review.selected.size))
      }
    }
  }
}

@Composable
private fun ReviewHeader(
  review: MatchReviewState,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier) {
    Text(
      text = stringResource(Res.string.review_title),
      style = MaterialTheme.typography.titleLarge,
    )
    val confidence = review.match.confidence
    Text(
      text = if (confidence != null) {
        stringResource(Res.string.review_from_confidence, review.providerName, (confidence * 100).roundToInt())
      } else {
        stringResource(Res.string.review_from, review.providerName)
      },
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

/** The ways of choosing, and how many are chosen, which goes under them when they don't fit beside */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectionModes(
  review: MatchReviewState,
  enabled: Boolean,
  onSelect: (SelectionMode) -> Unit,
  modifier: Modifier = Modifier,
) {
  FlowRow(
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
    itemVerticalAlignment = Alignment.CenterVertically,
    modifier = modifier.fillMaxWidth(),
  ) {
    SingleChoiceSegmentedButtonRow {
      val modes = listOf(
        SelectionMode.All to Res.string.review_all,
        SelectionMode.EmptyOnly to Res.string.review_empty_only,
        SelectionMode.None to Res.string.review_none,
      )
      modes.forEachIndexed { index, (mode, label) ->
        SegmentedButton(
          selected = review.mode == mode,
          onClick = { onSelect(mode) },
          enabled = enabled,
          shape = SegmentedButtonDefaults.itemShape(index, modes.size),
          modifier = Modifier.testTag("review_mode_${mode.name}"),
        ) {
          // Unwrapped, as the buttons are as wide as the widest needs, or its longest word would be all
          Text(stringResource(label), maxLines = 1, softWrap = false)
        }
      }
    }
    Text(
      text = stringResource(Res.string.review_selected, review.selected.size, review.changing.size),
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.testTag("review_count"),
    )
  }
}

/**
 * The book's cover and the result's side by side, at the same size so they compare fairly, with how
 * big each image is once its file says, and the switch that takes the new one.
 */
@Composable
private fun CoverChange(
  row: MatchRow,
  currentSize: ImageSize?,
  newSize: ImageSize?,
  checked: Boolean,
  enabled: Boolean,
  canChangeCover: Boolean,
  onToggle: () -> Unit,
  maxCoverSize: Dp,
  modifier: Modifier = Modifier,
) {
  val new = (row.new as MatchValue.Image).url
  val current = (row.current as? MatchValue.Image)?.url
  Column(
    verticalArrangement = Arrangement.spacedBy(12.dp),
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 24.dp, vertical = 12.dp)
      .testTag("review_cover"),
  ) {
    Text(
      text = stringResource(Res.string.label_cover),
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.primary,
    )
    BoxWithConstraints(Modifier.fillMaxWidth()) {
      val side = minOf(maxCoverSize, (maxWidth - CoverArrowWidth) / 2)
      Row(verticalAlignment = Alignment.Top) {
        CoverSide(
          url = current,
          side = side,
          label = stringResource(
            if (current != null) Res.string.review_current_cover else Res.string.review_cover_none,
          ),
          contentDescription = stringResource(Res.string.cd_current_cover),
          size = currentSize,
          comparison = null,
        )
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .width(CoverArrowWidth)
            .height(side),
        ) {
          Icon(
            imageVector = CampfireIcons.Rounded.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        CoverSide(
          url = new,
          side = side,
          label = stringResource(Res.string.review_new_cover),
          contentDescription = stringResource(Res.string.cd_new_cover),
          size = newSize,
          comparison = compare(newSize, currentSize)?.let { stringResource(it) },
        )
      }
    }
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .fillMaxWidth()
        .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = { onToggle() })
        .testTag("review_row_Cover"),
    ) {
      Text(
        text = stringResource(Res.string.review_use_new_cover),
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.weight(1f),
      )
      Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
    Text(
      text = stringResource(
        if (canChangeCover) Res.string.review_cover_no_undo else Res.string.review_cover_needs_upload,
      ),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun CoverSide(
  url: String?,
  side: Dp,
  label: String,
  contentDescription: String,
  size: ImageSize?,
  comparison: String?,
  modifier: Modifier = Modifier,
) {
  Column(
    verticalArrangement = Arrangement.spacedBy(4.dp),
    modifier = modifier.width(side),
  ) {
    CoverImage(
      imageUrl = url,
      contentDescription = contentDescription,
      placeholder = placeholderBookPainter(),
      size = side,
      shape = coverImageShape(side),
      modifier = Modifier
        .padding(bottom = 4.dp)
        .size(side),
    )
    Text(
      text = label,
      style = MaterialTheme.typography.labelLarge,
    )
    if (size != null) {
      Text(
        text = stringResource(Res.string.review_cover_size, size.width, size.height),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
    if (comparison != null) {
      Text(
        text = comparison,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
      )
    }
  }
}

/** Whether [new] is noticeably bigger or smaller than [current], when both are known */
private fun compare(new: ImageSize?, current: ImageSize?): StringResource? {
  if (new == null || current == null) return null
  return when {
    new.pixels > current.pixels * NoticeableSizeChange -> Res.string.review_cover_larger
    new.pixels * NoticeableSizeChange < current.pixels -> Res.string.review_cover_smaller
    else -> null
  }
}

@Composable
private fun ReviewRow(
  row: MatchRow,
  checked: Boolean,
  enabled: Boolean,
  merge: ListMerge,
  onToggle: () -> Unit,
  onMerge: (ListMerge) -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    modifier = modifier
      .fillMaxWidth()
      .toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = { onToggle() })
      .padding(horizontal = 24.dp, vertical = 10.dp)
      .testTag("review_row_${row.field.name}"),
  ) {
    Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
    Column(
      verticalArrangement = Arrangement.spacedBy(4.dp),
      modifier = Modifier.weight(1f),
    ) {
      Text(
        text = stringResource(row.field.label),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
      )
      Text(
        text = row.new.display(),
        style = MaterialTheme.typography.bodyLarge,
        maxLines = if (row.field == MatchField.Description) 4 else 2,
        overflow = TextOverflow.Ellipsis,
      )
      if (row.isListMergeable && checked && row.current != null) {
        MergeChoice(merge = merge, enabled = enabled, onMerge = onMerge)
      }
      Text(
        text = row.current?.let { stringResource(Res.string.review_currently, it.display()) }
          ?: stringResource(Res.string.review_currently_none),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun MergeChoice(
  merge: ListMerge,
  enabled: Boolean,
  onMerge: (ListMerge) -> Unit,
  modifier: Modifier = Modifier,
) {
  SingleChoiceSegmentedButtonRow(modifier = modifier) {
    listOf(ListMerge.Replace to Res.string.review_replace, ListMerge.Add to Res.string.review_add)
      .forEachIndexed { index, (option, label) ->
        SegmentedButton(
          selected = merge == option,
          onClick = { onMerge(option) },
          enabled = enabled,
          shape = SegmentedButtonDefaults.itemShape(index, 2),
          modifier = Modifier.testTag("review_merge_${option.name}"),
        ) {
          Text(stringResource(label))
        }
      }
  }
}

@Composable
private fun MatchValue.display(): String = when (this) {
  is MatchValue.Text -> text.plain()
  is MatchValue.Names -> names.joinToString()
  is MatchValue.Series -> entries.joinToString { entry -> entry.sequence?.let { "${entry.name} #$it" } ?: entry.name }
  is MatchValue.Flag -> stringResource(if (isOn) Res.string.value_yes else Res.string.value_no)
  is MatchValue.Image -> url
}

private val MatchField.label: StringResource
  get() = when (this) {
    MatchField.Cover -> Res.string.label_cover
    MatchField.Title -> Res.string.label_title
    MatchField.Subtitle -> Res.string.label_subtitle
    MatchField.Authors -> Res.string.label_authors
    MatchField.Narrators -> Res.string.label_narrators
    MatchField.Series -> Res.string.label_series_name
    MatchField.Description -> Res.string.label_description
    MatchField.Publisher -> Res.string.label_publisher
    MatchField.PublishedYear -> Res.string.label_published_year
    MatchField.Language -> Res.string.label_language
    MatchField.Genres -> Res.string.label_genres
    MatchField.Tags -> Res.string.label_tags
    MatchField.Isbn -> Res.string.label_isbn
    MatchField.Asin -> Res.string.label_asin
    MatchField.Explicit -> Res.string.label_explicit
    MatchField.Abridged -> Res.string.label_abridged
  }

/** The most a cover takes in the review, where two have to fit side by side */
private val CoverHeroSize = 140.dp
private val SideSheetCoverSize = 104.dp
private val CoverArrowWidth = 48.dp

/** A cover with this many times the pixels of the other, or fewer, reads as bigger or smaller */
private const val NoticeableSizeChange = 1.1
