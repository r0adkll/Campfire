package app.campfire.libraries.ui.detail.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.campfire.bookinfo.api.BookReview
import app.campfire.bookinfo.api.CommunityInfoState
import app.campfire.common.compose.extensions.rememberHtmlRichTextState
import app.campfire.common.compose.extensions.roundToSingleDecimal
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.community_review_by_author
import campfire.features.libraries.ui.generated.resources.community_review_sheet_title
import campfire.features.libraries.ui.generated.resources.community_review_show_spoiler
import campfire.features.libraries.ui.generated.resources.community_review_spoiler_warning
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.ui.material.RichText
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalRichTextApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ReviewBottomSheet(
  review: BookReview,
  state: CommunityInfoState,
  onOpenProvider: (String) -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val sheetState = rememberModalBottomSheetState()
  var spoilerRevealed by remember(review) { mutableStateOf(false) }
  val obscured = review.hasSpoilers && !spoilerRevealed

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier
        .padding(horizontal = 24.dp)
        .navigationBarsPadding(),
    ) {
      Text(
        text = stringResource(Res.string.community_review_sheet_title),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
          .align(Alignment.CenterHorizontally),
      )

      // Header
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
          .align(Alignment.CenterHorizontally),
      ) {
        review.avatarUrl?.let { avatarUrl ->
          ReviewerAvatar(url = avatarUrl, fallbackName = review.author, size = 24.dp)
          Spacer(Modifier.width(6.dp))
        }
        review.author?.let { author ->
          // Localized as a full "by %1$s" sentence (word order is language-
          // dependent); the author's span is bolded wherever it lands.
          val byAuthor = stringResource(Res.string.community_review_by_author, author)
          Text(
            text = buildAnnotatedString {
              append(byAuthor)
              val start = byAuthor.indexOf(author)
              if (start >= 0) {
                addStyle(
                  SpanStyle(fontWeight = FontWeight.SemiBold),
                  start,
                  start + author.length,
                )
              }
            },
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Spacer(Modifier.width(8.dp))
        }
        review.badge?.let { badge ->
          ReviewerBadge(text = badge, providerKey = state.providerId.key)
          Spacer(Modifier.width(8.dp))
        }
        review.rating?.let { rating ->
          Icon(
            imageVector = Icons.Rounded.Star,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp),
          )
          Spacer(Modifier.width(2.dp))
          Text(
            text = rating.roundToSingleDecimal(),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
          )
        }
      }

      Spacer(Modifier.height(16.dp))

      Box(
        modifier = Modifier.weight(1f, fill = false),
      ) {
        RichText(
          state = rememberHtmlRichTextState(review.text),
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .then(if (obscured) Modifier.blur(12.dp) else Modifier),
        )

        if (obscured) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
              .align(Alignment.Center)
              .padding(8.dp),
          ) {
            Text(
              text = stringResource(Res.string.community_review_spoiler_warning),
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
            TextButton(onClick = { spoilerRevealed = true }) {
              Text(stringResource(Res.string.community_review_show_spoiler))
            }
          }
        }
      }

      Spacer(Modifier.height(24.dp))
    }
  }
}
