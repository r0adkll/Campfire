package app.campfire.libraries.ui.detail.composables.slots

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
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
import app.campfire.common.compose.theme.PaytoneOneFontFamily
import app.campfire.common.compose.widgets.LibraryItemSharedTransitionKey
import app.campfire.core.model.LibraryItem
import app.campfire.libraries.ui.detail.LibraryItemUiEvent
import app.campfire.libraries.ui.detail.composables.AuthorNarratorBar
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.unknown_title
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import kotlin.time.Duration.Companion.milliseconds
import org.jetbrains.compose.resources.stringResource

class TitleAndAuthorSlot(
  private val libraryItem: LibraryItem,
  private val sharedTransitionKey: String,
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
        Icon(
          Icons.Outlined.Schedule,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
          text = libraryItem.media.durationInMillis.milliseconds.readoutFormat(),
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.width(26.dp))
      }

      Spacer(Modifier.height(24.dp))

      AuthorNarratorBar(
        author = libraryItem.media.metadata.authorName,
        narrator = libraryItem.media.metadata.narratorName,
        onAuthorClick = {
          eventSink(LibraryItemUiEvent.AuthorClick(libraryItem))
        },
        onNarratorClick = {
          eventSink(LibraryItemUiEvent.NarratorClick(libraryItem))
        },
      )
    }
  }
}
