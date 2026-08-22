// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui.composables

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.widgets.rememberDrawSizedRequest
import coil3.compose.AsyncImage

@Composable
internal fun Thumbnail(
  imageUrl: String?,
  contentDescription: String?,
  modifier: Modifier = Modifier,
  size: Dp = ThumbnailSize,
  cornerRadius: Dp = CornerRadius,
  borderWidth: Dp = BorderWidth,
  borderColor: Color = MaterialTheme.colorScheme.secondary,
) {
  val shape = RoundedCornerShape(cornerRadius)
  AsyncImage(
    // Request a sized rendition like CoverImage everywhere else, rather than passing the bare URL:
    // a content:// artwork URI from the media session only resolves through this path.
    model = rememberDrawSizedRequest(imageUrl, size),
    contentDescription = contentDescription,
    contentScale = ContentScale.Crop,
    modifier = modifier
      .size(size)
      .clip(shape)
      .border(borderWidth, borderColor, shape),
  )
}

private val ThumbnailSize = 56.dp
private val CornerRadius = 8.dp
private val BorderWidth = 1.dp
