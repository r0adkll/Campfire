// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.management.ui.cover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.ArrowBack
import app.campfire.common.compose.icons.rounded.ArrowForward
import app.campfire.common.compose.widgets.CoverImage
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.common.compose.widgets.coverImageShape
import app.campfire.common.compose.widgets.placeholderBookPainter
import app.campfire.common.compose.widgets.sheets.AdaptiveSheetOverlay
import app.campfire.common.compose.widgets.sheets.LocalSheetPresentation
import app.campfire.common.compose.widgets.sheets.SheetPresentation
import app.campfire.management.api.match.ImageSize
import app.campfire.management.ui.match.CoverSide
import app.campfire.management.ui.match.compareCoverSizes
import campfire.features.management.ui.generated.resources.Res
import campfire.features.management.ui.generated.resources.cd_current_cover
import campfire.features.management.ui.generated.resources.cd_new_cover
import campfire.features.management.ui.generated.resources.cd_next_cover
import campfire.features.management.ui.generated.resources.cd_previous_cover
import campfire.features.management.ui.generated.resources.cover_preview_cancel
import campfire.features.management.ui.generated.resources.cover_preview_count
import campfire.features.management.ui.generated.resources.cover_preview_single
import campfire.features.management.ui.generated.resources.cover_preview_use
import campfire.features.management.ui.generated.resources.review_cover_none
import campfire.features.management.ui.generated.resources.review_current_cover
import campfire.features.management.ui.generated.resources.review_new_cover
import com.slack.circuit.overlay.LocalOverlayHost
import kotlinx.coroutines.flow.filter
import org.jetbrains.compose.resources.stringResource

/**
 * Shows the cover being looked at in an adaptive sheet while there is one, and tells the section
 * when the sheet's put away.
 */
@Composable
internal fun CoverPreviewSheet(
  state: CoverUiState,
  enabled: Boolean,
) {
  val overlayHost = LocalOverlayHost.current
  val latestState by rememberUpdatedState(state)
  val latestEnabled by rememberUpdatedState(enabled)
  LaunchedEffect(overlayHost) {
    snapshotFlow { latestState.preview != null }
      .filter { it }
      .collect {
        overlayHost.show(
          AdaptiveSheetOverlay(model = Unit, onDismiss = {}, skipPartiallyExpanded = true) { _, navigator ->
            val preview = latestState.preview
            // The cover shown last stays while the sheet slides away
            val shown = remember { arrayOfNulls<CoverPreview>(1) }
            if (preview != null) shown[0] = preview
            LaunchedEffect(preview == null) {
              if (preview == null) navigator.finish(Unit)
            }
            shown[0]?.let {
              CoverPreviewContent(
                preview = it,
                currentUrl = latestState.currentUrl,
                currentSize = latestState.currentSize,
                canUse = latestEnabled && latestState.canUseAddresses,
                eventSink = latestState.eventSink,
              )
            }
          },
        )
        latestState.eventSink(CoverUiEvent.ClosePreview)
      }
  }
}

/** A cover large, beside the book's own, to page through and choose */
@Composable
internal fun CoverPreviewContent(
  preview: CoverPreview,
  currentUrl: String?,
  currentSize: ImageSize?,
  canUse: Boolean,
  eventSink: (CoverUiEvent) -> Unit,
  modifier: Modifier = Modifier,
) {
  val isSideSheet = LocalSheetPresentation.current == SheetPresentation.Side
  Column(
    verticalArrangement = Arrangement.spacedBy(16.dp),
    modifier = modifier
      .then(if (isSideSheet) Modifier.width(SideSheetWidth) else Modifier.fillMaxWidth())
      .navigationBarsPadding()
      .padding(bottom = 16.dp)
      .testTag("cover_preview"),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 8.dp),
    ) {
      Text(
        text = if (preview.covers.size > 1) {
          stringResource(Res.string.cover_preview_count, preview.index + 1, preview.covers.size)
        } else {
          stringResource(Res.string.cover_preview_single)
        },
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.weight(1f),
      )
      if (preview.covers.size > 1) {
        val previousLabel = stringResource(Res.string.cd_previous_cover)
        IconButtonTooltip(text = previousLabel) {
          IconButton(
            onClick = { eventSink(CoverUiEvent.PageTo(preview.index - 1)) },
            enabled = preview.index > 0,
            modifier = Modifier.testTag("cover_preview_previous"),
          ) {
            Icon(CampfireIcons.Rounded.ArrowBack, contentDescription = previousLabel)
          }
        }
        val nextLabel = stringResource(Res.string.cd_next_cover)
        IconButtonTooltip(text = nextLabel) {
          IconButton(
            onClick = { eventSink(CoverUiEvent.PageTo(preview.index + 1)) },
            enabled = preview.index < preview.covers.lastIndex,
            modifier = Modifier.testTag("cover_preview_next"),
          ) {
            Icon(CampfireIcons.Rounded.ArrowForward, contentDescription = nextLabel)
          }
        }
      }
    }

    // Only the covers scroll, so the buttons stay in reach in a short window
    Column(
      verticalArrangement = Arrangement.spacedBy(16.dp),
      modifier = Modifier
        .weight(1f, fill = false)
        .verticalScroll(rememberScrollState()),
    ) {
      BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 24.dp),
      ) {
        val side = minOf(maxWidth, PreviewMaxSize)
        CoverImage(
          imageUrl = preview.url,
          contentDescription = stringResource(Res.string.cd_new_cover),
          size = side,
          placeholder = placeholderBookPainter(),
          shape = coverImageShape(side),
          modifier = Modifier.size(side),
        )
      }

      Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.padding(horizontal = 24.dp),
      ) {
        CoverSide(
          url = currentUrl,
          side = CompareSize,
          label = stringResource(
            if (currentUrl != null) Res.string.review_current_cover else Res.string.review_cover_none,
          ),
          contentDescription = stringResource(Res.string.cd_current_cover),
          size = currentSize,
          comparison = null,
        )
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .width(48.dp)
            .height(CompareSize),
        ) {
          Icon(
            imageVector = CampfireIcons.Rounded.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        CoverSide(
          url = preview.url,
          side = CompareSize,
          label = stringResource(Res.string.review_new_cover),
          contentDescription = stringResource(Res.string.cd_new_cover),
          size = preview.size,
          comparison = compareCoverSizes(preview.size, currentSize)?.let { stringResource(it) },
        )
      }
    }

    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
    ) {
      TextButton(onClick = { eventSink(CoverUiEvent.ClosePreview) }) {
        Text(stringResource(Res.string.cover_preview_cancel))
      }
      Button(
        onClick = { eventSink(CoverUiEvent.UsePreviewed) },
        enabled = canUse,
        modifier = Modifier.testTag("cover_preview_use"),
      ) {
        Text(stringResource(Res.string.cover_preview_use))
      }
    }
  }
}

private val PreviewMaxSize = 320.dp
private val CompareSize = 96.dp
private val SideSheetWidth = 400.dp
