// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.downloads.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.campfire.core.extensions.asReadableBytes
import app.campfire.downloads.ui.DownloadKind
import app.campfire.downloads.ui.DownloadsSummary
import campfire.features.downloads.ui.generated.resources.Res
import campfire.features.downloads.ui.generated.resources.downloads_filter_all
import campfire.features.downloads.ui.generated.resources.downloads_filter_count
import campfire.features.downloads.ui.generated.resources.downloads_section_audiobooks
import campfire.features.downloads.ui.generated.resources.downloads_section_count
import campfire.features.downloads.ui.generated.resources.downloads_section_ebooks
import campfire.features.downloads.ui.generated.resources.downloads_section_podcasts
import campfire.features.downloads.ui.generated.resources.downloads_summary
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * How many downloads there are and what they take up, with a bar splitting that by kind. With
 * more than one kind downloaded, the kinds are also chips that narrow the list to one of them.
 */
@Composable
internal fun DownloadsSummaryHeader(
  summary: DownloadsSummary,
  filter: DownloadKind?,
  onFilterChange: (DownloadKind?) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text(
      text = pluralStringResource(
        Res.plurals.downloads_summary,
        summary.count,
        summary.count,
        summary.sizeInBytes.asReadableBytes(),
      ),
      style = MaterialTheme.typography.titleLarge,
      modifier = Modifier.semantics { heading() },
    )

    if (summary.sizeInBytes > 0L) {
      StorageBar(summary)
    }

    if (summary.kinds.size > 1) {
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState()),
      ) {
        FilterChip(
          selected = filter == null,
          onClick = { onFilterChange(null) },
          label = { Text(stringResource(Res.string.downloads_filter_all)) },
        )
        summary.kinds.forEach { kind ->
          FilterChip(
            selected = filter == kind.kind,
            // Choosing the kind already shown goes back to all of them
            onClick = { onFilterChange(kind.kind.takeUnless { it == filter }) },
            label = {
              Text(stringResource(Res.string.downloads_filter_count, stringResource(kind.kind.label), kind.count))
            },
            leadingIcon = {
              Box(
                Modifier
                  .size(8.dp)
                  .background(kind.kind.color, CircleShape),
              )
            },
          )
        }
      }
    }
  }
}

/** What each kind takes up, as its share of a bar. The chips under it are its legend. */
@Composable
private fun StorageBar(
  summary: DownloadsSummary,
  modifier: Modifier = Modifier,
) {
  Row(
    horizontalArrangement = Arrangement.spacedBy(2.dp),
    modifier = modifier
      .fillMaxWidth()
      .height(8.dp)
      .clip(CircleShape)
      // Says nothing the heading and the chips don't
      .clearAndSetSemantics {},
  ) {
    // A kind taking up next to nothing still shows as a sliver
    val smallest = summary.sizeInBytes * MinBarShare
    summary.kinds.forEach { kind ->
      Box(
        Modifier
          .weight(maxOf(kind.sizeInBytes.toFloat(), smallest, 1f))
          .fillMaxHeight()
          .background(kind.kind.color),
      )
    }
  }
}

/**
 * A section's name with what its downloads take up beside it, when that's known, and how many
 * there are at the end of the line.
 */
@Composable
internal fun DownloadSectionHeader(
  title: String,
  count: Int,
  sizeInBytes: Long,
  modifier: Modifier = Modifier,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
      .fillMaxWidth()
      // Opaque, as rows scroll under it while it's stuck to the top
      .background(MaterialTheme.colorScheme.surface)
      .padding(vertical = 8.dp),
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleSmall,
      modifier = Modifier.semantics { heading() },
    )
    if (sizeInBytes > 0L) {
      Spacer(Modifier.size(8.dp))
      Text(
        text = sizeInBytes.asReadableBytes(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Spacer(Modifier.weight(1f))
    Text(
      text = stringResource(Res.string.downloads_section_count, count),
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

internal val DownloadKind.label: StringResource
  get() = when (this) {
    DownloadKind.Audiobook -> Res.string.downloads_section_audiobooks
    DownloadKind.Podcast -> Res.string.downloads_section_podcasts
    DownloadKind.Ebook -> Res.string.downloads_section_ebooks
  }

/**
 * Steps of the one color, as a theme's primary, secondary and tertiary can be too alike to tell
 * apart side by side.
 */
private val DownloadKind.color: Color
  @Composable get() = MaterialTheme.colorScheme.primary.copy(
    alpha = when (this) {
      DownloadKind.Audiobook -> 1f
      DownloadKind.Podcast -> 0.6f
      DownloadKind.Ebook -> 0.3f
    },
  )

private const val MinBarShare = 0.02f
