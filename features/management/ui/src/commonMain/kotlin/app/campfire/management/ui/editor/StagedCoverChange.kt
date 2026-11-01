// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.management.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.ArrowForward
import app.campfire.common.compose.widgets.CoverImage
import app.campfire.common.compose.widgets.coverImageShape
import app.campfire.common.compose.widgets.placeholderBookPainter
import app.campfire.libraries.api.CoverUpdate
import campfire.features.management.ui.generated.resources.Res
import campfire.features.management.ui.generated.resources.action_keep_cover
import campfire.features.management.ui.generated.resources.cd_current_cover
import campfire.features.management.ui.generated.resources.cd_new_cover
import campfire.features.management.ui.generated.resources.new_cover_message
import campfire.features.management.ui.generated.resources.new_cover_message_file
import campfire.features.management.ui.generated.resources.new_cover_message_removed
import campfire.features.management.ui.generated.resources.new_cover_title
import campfire.features.management.ui.generated.resources.new_cover_title_removed
import campfire.features.management.ui.generated.resources.review_cover_none
import campfire.features.management.ui.generated.resources.review_current_cover
import campfire.features.management.ui.generated.resources.review_new_cover
import org.jetbrains.compose.resources.stringResource

/**
 * The cover [staged] to take the place of the book's own, at [currentUrl], once the details are
 * saved: the two side by side, what saving does, and a way to keep the current one.
 */
@Composable
internal fun StagedCoverChange(
  staged: StagedCover,
  currentUrl: String?,
  enabled: Boolean,
  onKeepCurrent: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val removing = staged.update == CoverUpdate.Remove
  Column(
    verticalArrangement = Arrangement.spacedBy(8.dp),
    modifier = modifier
      .fillMaxWidth()
      .testTag("editor_new_cover"),
  ) {
    FieldLabel(
      text = stringResource(if (removing) Res.string.new_cover_title_removed else Res.string.new_cover_title),
      changed = true,
    )
    Row(verticalAlignment = Alignment.Top) {
      LabeledCover(
        url = currentUrl,
        label = stringResource(
          if (currentUrl != null) Res.string.review_current_cover else Res.string.review_cover_none,
        ),
        contentDescription = stringResource(Res.string.cd_current_cover),
      )
      Icon(
        imageVector = CampfireIcons.Rounded.ArrowForward,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
          .padding(horizontal = 12.dp)
          .padding(top = StagedCoverSize / 2 - 12.dp),
      )
      LabeledCover(
        url = staged.previewUrl,
        label = stringResource(if (removing) Res.string.review_cover_none else Res.string.review_new_cover),
        contentDescription = stringResource(Res.string.cd_new_cover),
      )
    }
    Text(
      text = when {
        removing -> stringResource(Res.string.new_cover_message_removed)
        staged.fileName != null -> stringResource(Res.string.new_cover_message_file, staged.fileName)
        else -> stringResource(Res.string.new_cover_message)
      },
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    TextButton(
      onClick = onKeepCurrent,
      enabled = enabled,
      modifier = Modifier.testTag("editor_keep_cover"),
    ) {
      Text(stringResource(Res.string.action_keep_cover))
    }
  }
}

/** A cover with what it is under it, like "Current" or "New" */
@Composable
internal fun LabeledCover(
  url: String?,
  label: String,
  contentDescription: String,
  modifier: Modifier = Modifier,
  size: Dp = StagedCoverSize,
) {
  Column(
    verticalArrangement = Arrangement.spacedBy(4.dp),
    modifier = modifier,
  ) {
    CoverImage(
      imageUrl = url,
      contentDescription = contentDescription,
      size = size,
      placeholder = placeholderBookPainter(),
      shape = coverImageShape(size),
      modifier = Modifier.size(size),
    )
    Text(
      text = label,
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

private val StagedCoverSize = 96.dp
