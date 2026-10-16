// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail.composables.slots

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.extensions.readoutFormat
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.MenuBook
import app.campfire.common.compose.icons.rounded.Schedule
import app.campfire.common.compose.icons.rounded.Sensors
import app.campfire.common.compose.theme.PaytoneOneFontFamily
import app.campfire.common.compose.widgets.LibraryItemSharedTransitionKey
import app.campfire.core.extensions.asReadableBytes
import app.campfire.core.model.LibraryItem
import app.campfire.libraries.ui.detail.LibraryItemUiEvent
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.ebook_format_and_size
import campfire.features.libraries.ui.generated.resources.streaming_badge_hls
import campfire.features.libraries.ui.generated.resources.unknown_title
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import kotlin.time.Duration.Companion.milliseconds
import org.jetbrains.compose.resources.stringResource

class TitleSlot(
  private val libraryItem: LibraryItem,
  private val sharedTransitionKey: String,
  @get:VisibleForTesting val showHlsBadge: Boolean = false,
) : ContentSlot {

  override val id: String = "title_author"

  @OptIn(ExperimentalSharedTransitionApi::class)
  @Composable
  override fun Content(modifier: Modifier, eventSink: (LibraryItemUiEvent) -> Unit) = SharedElementTransitionScope {
    Column(
      modifier = modifier,
    ) {
      Text(
        text = libraryItem.media.metadata.title
          ?: stringResource(Res.string.unknown_title),
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.SemiBold,
        fontStyle = if (libraryItem.media.metadata.title == null) FontStyle.Italic else null,
        fontFamily = PaytoneOneFontFamily,
        textAlign = TextAlign.Center,
        modifier = Modifier
          .fillMaxWidth()
          .sharedBounds(
            sharedContentState = rememberSharedContentState(
              LibraryItemSharedTransitionKey(
                id = sharedTransitionKey,
                type = LibraryItemSharedTransitionKey.ElementType.Title,
              ),
            ),
            animatedVisibilityScope = requireAnimatedScope(SharedElementTransitionScope.AnimatedScope.Navigation),
          )
          .padding(horizontal = 16.dp),
      )

      libraryItem.media.metadata.subtitle?.let { subtitle ->
        Text(
          text = subtitle,
          style = MaterialTheme.typography.titleLarge,
          textAlign = TextAlign.Center,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        )
      }

      Spacer(Modifier.height(4.dp))

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp)
          .alpha(0.65f),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        // An ebook has no length to listen to, so it shows what it's read in and how big it is
        val ebookFormat = libraryItem.media.ebookFormat?.takeIf { libraryItem.isEbookOnly }
        Icon(
          if (ebookFormat != null) CampfireIcons.Rounded.MenuBook else CampfireIcons.Rounded.Schedule,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
          text = when {
            ebookFormat == null -> libraryItem.media.durationInMillis.milliseconds.readoutFormat()
            libraryItem.media.sizeInBytes > 0L -> stringResource(
              Res.string.ebook_format_and_size,
              ebookFormat.uppercase(),
              libraryItem.media.sizeInBytes.asReadableBytes(),
            )
            else -> ebookFormat.uppercase()
          },
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
        )
        if (showHlsBadge) {
          Spacer(Modifier.width(12.dp))
          Icon(
            CampfireIcons.Rounded.Sensors,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(Modifier.width(4.dp))
          Text(
            text = stringResource(Res.string.streaming_badge_hls),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
          )
        }
        Spacer(Modifier.width(26.dp))
      }

      Spacer(Modifier.height(24.dp))
    }
  }
}
