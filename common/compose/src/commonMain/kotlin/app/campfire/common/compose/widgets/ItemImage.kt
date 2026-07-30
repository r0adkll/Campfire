// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter

@Composable
fun ItemImage(
  imageUrl: String,
  contentDescription: String?,
  modifier: Modifier = Modifier,
  contentScale: ContentScale = ContentScale.Fit,
) {
  Box(
    modifier = modifier,
  ) {
    val painter = rememberAsyncImagePainter(imageUrl)

    val imageState by painter.state.collectAsState()
    when (imageState) {
      // The raw throwable message embeds the full image URL (user's server host),
      // which would leak through the accessibility tree — keep this static.
      is AsyncImagePainter.State.Error -> ImageError(
        errorMessage = "Image failed to load",
      )
      is AsyncImagePainter.State.Loading -> ImageLoading()
      // Do nothing in the other states
      else -> Unit
    }

    Image(
      painter,
      contentDescription = contentDescription,
      contentScale = contentScale,
      modifier = Modifier
        .fillMaxSize(),
    )
  }
}

@Composable
internal fun ImageError(
  errorMessage: String,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .background(MaterialTheme.colorScheme.errorContainer)
      .fillMaxSize(),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    CompositionLocalProvider(
      LocalContentColor provides MaterialTheme.colorScheme.onErrorContainer,
    ) {
      Icon(
        Icons.Rounded.WarningAmber,
        contentDescription = errorMessage,
        modifier = Modifier.size(32.dp),
      )
    }
  }
}

@Composable
internal fun ImageLoading(
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .background(MaterialTheme.colorScheme.surfaceContainerHighest)
      .fillMaxSize(),
    contentAlignment = Alignment.Center,
  ) {
    CircularProgressIndicator()
  }
}
