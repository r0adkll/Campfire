// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.providerBrandColor
import coil3.compose.rememberAsyncImagePainter

/**
 * Circular reviewer profile image. Falls back to a monogram (or person glyph)
 * when the reviewer has no image, so layouts reserve the same space either way.
 */
@Composable
internal fun ReviewerAvatar(
  url: String?,
  fallbackName: String?,
  modifier: Modifier = Modifier,
  size: Dp = 32.dp,
) {
  if (url != null) {
    Image(
      painter = rememberAsyncImagePainter(url),
      contentDescription = null,
      contentScale = ContentScale.Crop,
      modifier = modifier
        .size(size)
        .clip(CircleShape),
    )
  } else {
    Box(
      contentAlignment = Alignment.Center,
      modifier = modifier
        .size(size)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.secondaryContainer),
    ) {
      val initial = fallbackName?.firstOrNull { it.isLetterOrDigit() }?.uppercase()
      if (initial != null) {
        Text(
          text = initial,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
      } else {
        Icon(
          imageVector = Icons.Rounded.Person,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSecondaryContainer,
          modifier = Modifier.size(size * 0.6f),
        )
      }
    }
  }
}

/**
 * Provider-issued profile badge (e.g. Hardcover's "Supporter" flair), tinted
 * with the provider's brand color when one is defined.
 */
@Composable
internal fun ReviewerBadge(
  text: String,
  providerKey: String,
  modifier: Modifier = Modifier,
) {
  val brandColor = providerBrandColor(providerKey)
  Surface(
    shape = CircleShape,
    color = brandColor?.copy(alpha = 0.15f) ?: MaterialTheme.colorScheme.secondaryContainer,
    contentColor = brandColor ?: MaterialTheme.colorScheme.onSecondaryContainer,
    modifier = modifier,
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall,
      maxLines = 1,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
    )
  }
}
