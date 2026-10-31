// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Public-domain audiobooks for the preview. Their covers are fixed gradients, like real cover art,
 * so they stay the same whatever theme is being previewed.
 */
internal enum class SampleBook(
  val title: String,
  val author: String,
  private val coverTop: Color,
  private val coverBottom: Color,
) {
  MobyDick("Moby-Dick", "Herman Melville", Color(0xFF2A6175), Color(0xFF0B2530)),
  PrideAndPrejudice("Pride and Prejudice", "Jane Austen", Color(0xFFD29A96), Color(0xFF7A3E4A)),
  Dracula("Dracula", "Bram Stoker", Color(0xFF6E1527), Color(0xFF1A0408)),
  Frankenstein("Frankenstein", "Mary Shelley", Color(0xFF5D7246), Color(0xFF1C2414)),
  TimeMachine("The Time Machine", "H. G. Wells", Color(0xFFE0AD4B), Color(0xFF7C4A12)),
  ;

  val coverBrush: Brush get() = Brush.linearGradient(listOf(coverTop, coverBottom))
}

internal const val SampleNarrator = "Karen Savage"

internal const val SampleServerUrl = "https://abs.example.net"

/** A stand-in cover. It's decorative, so screen readers skip it; the title appears next to it. */
@Composable
internal fun SampleCover(
  book: SampleBook,
  modifier: Modifier = Modifier,
  showTitle: Boolean = true,
) {
  Box(
    modifier = modifier
      .aspectRatio(1f)
      .clip(MaterialTheme.shapes.medium)
      .background(book.coverBrush)
      .padding(8.dp)
      .clearAndSetSemantics {},
    contentAlignment = Alignment.BottomStart,
  ) {
    if (showTitle) {
      Text(
        text = book.title,
        style = MaterialTheme.typography.labelMedium.copy(
          fontWeight = FontWeight.Bold,
          shadow = Shadow(color = Color.Black.copy(alpha = 0.4f), blurRadius = 4f),
        ),
        color = Color.White,
        maxLines = 3,
      )
    }
  }
}
