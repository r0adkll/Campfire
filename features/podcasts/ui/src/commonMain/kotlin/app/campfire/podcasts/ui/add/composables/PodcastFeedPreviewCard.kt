package app.campfire.podcasts.ui.add.composables

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.extensions.thenIfNotNull
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Podcasts
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.common.compose.widgets.CoverImage
import app.campfire.podcasts.api.PodcastDraft
import app.campfire.podcasts.ui.AddPodcastSharedTransitionKey
import campfire.features.podcasts.ui.generated.resources.Res
import campfire.features.podcasts.ui.generated.resources.add_podcast_feed_continue
import campfire.features.podcasts.ui.generated.resources.add_podcast_result_cover
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalSharedTransitionApi::class)
@Composable
internal fun PodcastFeedPreviewCard(
  draft: PodcastDraft,
  onContinue: () -> Unit,
  modifier: Modifier = Modifier,
  sharedTransitionKey: String = draft.feedUrl,
) = SharedElementTransitionScope {
  val animationScope = findAnimatedScope(SharedElementTransitionScope.AnimatedScope.Navigation)

  ElevatedCard(
    modifier = modifier
      .fillMaxWidth()
      .thenIfNotNull(animationScope) { scope ->
        sharedBounds(
          sharedContentState = rememberSharedContentState(
            AddPodcastSharedTransitionKey(
              id = sharedTransitionKey,
              type = AddPodcastSharedTransitionKey.ElementType.Bounds,
            ),
          ),
          animatedVisibilityScope = scope,
        )
      },
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.elevatedCardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ),
  ) {
    Column {
      Row(verticalAlignment = Alignment.Top) {
        val cover = draft.coverUrl
        if (!cover.isNullOrBlank()) {
          CoverImage(
            imageUrl = cover,
            contentDescription = stringResource(Res.string.add_podcast_result_cover, draft.title),
            size = ThumbnailSize,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
              .padding(
                start = 16.dp,
                top = 16.dp,
              ),
            sharedElementModifier = Modifier.thenIfNotNull(animationScope) { scope ->
              sharedElement(
                sharedContentState = rememberSharedContentState(
                  AddPodcastSharedTransitionKey(
                    id = sharedTransitionKey,
                    type = AddPodcastSharedTransitionKey.ElementType.Cover,
                  ),
                ),
                animatedVisibilityScope = scope,
              )
            },
          )
        }

        Column(
          modifier = Modifier
            .weight(1f)
            .padding(16.dp),
        ) {
          Text(
            text = draft.title,
            style = MaterialTheme.typography.titleMediumEmphasized,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
          val author = draft.author
          if (!author.isNullOrBlank()) {
            Text(
              text = author,
              style = MaterialTheme.typography.labelSmallEmphasized,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          val description = draft.descriptionPlain
          if (!description.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
              text = description,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
              maxLines = 3,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }

      val continueButtonSize = ButtonDefaults.MinHeight
      Button(
        onClick = onContinue,
        shapes = ButtonDefaults.shapes(),
        contentPadding = ButtonDefaults.contentPaddingFor(continueButtonSize, hasStartIcon = true),
        modifier = Modifier
          .heightIn(continueButtonSize)
          .align(Alignment.End)
          .padding(
            end = 16.dp,
            bottom = 16.dp,
          ),
      ) {
        Icon(
          CampfireIcons.Rounded.Podcasts,
          contentDescription = null,
          modifier = Modifier.size(ButtonDefaults.iconSizeFor(continueButtonSize)),
        )
        Spacer(Modifier.width(ButtonDefaults.iconSpacingFor(continueButtonSize)))
        Text(
          text = stringResource(Res.string.add_podcast_feed_continue),
          style = ButtonDefaults.textStyleFor(continueButtonSize),
        )
      }
    }
  }
}

@Preview
@Composable
fun PodcastFeedPreviewCardPreview() {
  CampfireTheme {
    Scaffold {
      PodcastFeedPreviewCard(
        draft = PodcastDraft(
          title = "The Ezra Klein Show",
          author = "New York Times Opinion",
          descriptionHtml = null,
          descriptionPlain = "Ezra Klein invites you into conversations on topics that matter: " +
            "climate change, markets, politics, psychedelics, sci-fi, and food systems.",
          coverUrl = "https://image.simplecastcdn.com/images/47913775-afcf-4643-935f-31f1dcc86cfb" +
            "/6a0e5e15-04a3-47c8-9c50-2f2fe8e84437/3000x3000/teks-album-20art-3000px.jpg" +
            "?aid=rss_feed",
          feedUrl = "https://feeds.simplecast.com/82FI35Px",
          itunesId = null,
          itunesArtistId = null,
          itunesPageUrl = null,
          releaseDateIso = "2026-05-15T13:05:19Z",
          language = "en",
          genres = listOf("Society & Culture", "News", "Government"),
          explicit = false,
          episodeType = "episodic",
        ),
        onContinue = {},
        modifier = Modifier.padding(16.dp),
      )
    }
  }
}
