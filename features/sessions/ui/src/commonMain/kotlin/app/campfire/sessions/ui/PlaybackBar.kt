package app.campfire.sessions.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.extensions.readoutFormat
import app.campfire.common.compose.widgets.CoverImage
import app.campfire.sessions.api.models.Session
import app.campfire.sessions.ui.PlaybackBarState.Collapsed
import app.campfire.sessions.ui.PlaybackBarState.Expanded
import app.campfire.sessions.ui.PlaybackBarState.Hidden
import campfire.features.sessions.ui.generated.resources.Res
import campfire.features.sessions.ui.generated.resources.time_remaining
import coil3.compose.rememberAsyncImagePainter
import org.jetbrains.compose.resources.stringResource

enum class PlaybackBarState {
  Hidden,
  Collapsed,
  Expanded,
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PlaybackBar(
  expanded: Boolean,
  onExpansionChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  SessionHostLayout { currentSession ->
    SharedTransitionLayout(
      modifier = modifier,
    ) {
      AnimatedContent(
        targetState = when {
          currentSession == null -> Hidden
          expanded -> Expanded
          else -> Collapsed
        },
        transitionSpec = {
          when {
            (initialState == Hidden && targetState == Collapsed) ||
              (initialState == Collapsed && targetState == Hidden)
            -> slideInVertically { it } togetherWith slideOutVertically { it }

            else -> scaleIn() togetherWith scaleOut()
          }
        },
      ) { state ->
        when (state) {
          Hidden -> Unit
          Collapsed -> {
            PlaybackBar(
              session = currentSession!!,
              onClick = { onExpansionChange(!expanded) },
              sharedTransitionScope = this@SharedTransitionLayout,
              animatedVisibilityScope = this,
              modifier = Modifier.padding(8.dp),
            )
          }

          Expanded -> {
            ExpandedPlaybackBar(
              session = currentSession!!,
              sharedTransitionScope = this@SharedTransitionLayout,
              animatedVisibilityScope = this,
              onClose = { onExpansionChange(false) },
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ExpandedPlaybackBar(
  session: Session,
  onClose: () -> Unit,
  sharedTransitionScope: SharedTransitionScope,
  animatedVisibilityScope: AnimatedVisibilityScope,
  modifier: Modifier = Modifier,
) = with(sharedTransitionScope) {
  Surface(
    color = MaterialTheme.colorScheme.secondaryContainer,
    modifier = modifier
      .fillMaxSize()
      .sharedBounds(
        rememberSharedContentState(SharedBounds),
        animatedVisibilityScope = animatedVisibilityScope,
      ),
  ) {
    Column {
      TopAppBar(
        title = {},
        navigationIcon = {
          IconButton(
            onClick = onClose,
          ) {
            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null)
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.secondaryContainer,
          navigationIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
          actionIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
      )

      Column(
        Modifier.weight(1f),
      ) {
        Spacer(Modifier.height(16.dp))

        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.Center,
        ) {
          CoverImage(
            imageUrl = session.libraryItem.media.coverImageUrl,
            contentDescription = session.libraryItem.media.metadata.title,
            modifier = Modifier
              .sharedElement(
                rememberSharedContentState(SharedImage),
                animatedVisibilityScope = animatedVisibilityScope,
              )
              .fillMaxWidth()
              .padding(horizontal = 24.dp),
          )

          Spacer(Modifier.height(16.dp))

          // TODO: Dynamically compute the chapter and localized playback information based on the
          //  current session info.
          Text(
            text = session.libraryItem.media.metadata.title ?: "Unknown",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier
              .align(Alignment.CenterHorizontally)
              .padding(horizontal = 24.dp),
          )
        }

        var sliderValue by remember { mutableStateOf(0f) }
        Slider(
          value = sliderValue,
          onValueChange = { sliderValue = it },
          onValueChangeFinished = {
          },
          colors = SliderDefaults.colors(
            inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainer,
          ),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        )
        Row(
          Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp),
        ) {
          Text(
            text = "00:00",
            style = MaterialTheme.typography.labelSmall,
          )

          Spacer(Modifier.weight(1f))

          Text(
            text = session.duration.readoutFormat(),
            style = MaterialTheme.typography.labelSmall,
          )
        }

        Spacer(Modifier.height(32.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        ) {
          IconButton(
            onClick = {},
          ) {
            Icon(
              Icons.Rounded.SkipPrevious,
              modifier = Modifier.size(48.dp),
              contentDescription = null,
            )
          }

          IconButton(
            onClick = {},
          ) {
            Icon(
              Icons.Rounded.Replay10,
              modifier = Modifier.size(48.dp),
              contentDescription = null,
            )
          }

          Surface(
            shape = CircleShape,
            modifier = Modifier.size(90.dp),
            shadowElevation = 4.dp,
            onClick = {
            },
          ) {
            Box(
              modifier = Modifier.fillMaxSize(),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                Icons.Rounded.PlayArrow,
                modifier = Modifier.size(48.dp),
                contentDescription = null,
              )
            }
          }

          IconButton(
            onClick = {},
          ) {
            Icon(
              Icons.Rounded.Forward10,
              modifier = Modifier.size(48.dp),
              contentDescription = null,
            )
          }

          IconButton(
            onClick = {},
          ) {
            Icon(
              Icons.Rounded.SkipNext,
              modifier = Modifier.size(48.dp),
              contentDescription = null,
            )
          }
        }
      }

      Spacer(Modifier.height(24.dp))

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(72.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
      ) {
        IconButton(
          onClick = {},
        ) {
          Icon(Icons.Rounded.BookmarkAdd, contentDescription = null)
        }
        IconButton(
          onClick = {},
        ) {
          Icon(Icons.Rounded.Speed, contentDescription = null)
        }
        IconButton(
          onClick = {},
        ) {
          Icon(Icons.Rounded.Timer, contentDescription = null)
        }
        IconButton(
          onClick = {},
        ) {
          Icon(Icons.AutoMirrored.Rounded.List, contentDescription = null)
        }
      }

      Spacer(Modifier.navigationBarsPadding())
    }
  }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun PlaybackBar(
  session: Session,
  onClick: () -> Unit,
  sharedTransitionScope: SharedTransitionScope,
  animatedVisibilityScope: AnimatedVisibilityScope,
  modifier: Modifier = Modifier,
) = with(sharedTransitionScope) {
  Surface(
    color = MaterialTheme.colorScheme.secondaryContainer,
    shape = RoundedCornerShape(12.dp),
    modifier = modifier
      .sharedBounds(
        rememberSharedContentState(SharedBounds),
        animatedVisibilityScope = animatedVisibilityScope,
      ),
  ) {
    Box(
      Modifier.clickable(
        onClick = onClick,
      ),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Thumbnail(
          imageUrl = session.libraryItem.media.coverImageUrl,
          contentDescription = session.libraryItem.media.metadata.title,
          modifier = Modifier
            .sharedElement(
              rememberSharedContentState(SharedImage),
              animatedVisibilityScope = animatedVisibilityScope,
            )
            .padding(4.dp),
        )

        Spacer(Modifier.width(16.dp))

        Column(
          modifier = Modifier.weight(1f),
        ) {
          Text(
            text = session.libraryItem.media.metadata.title ?: "",
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            modifier = Modifier.basicMarquee(),
          )

          Text(
            text = stringResource(Res.string.time_remaining, session.timeRemaining.readoutFormat()),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.typography.labelSmall.color.copy(0.7f),
          )
        }

        Spacer(Modifier.width(16.dp))

        IconButton(
          onClick = {},
        ) {
          Icon(
            Icons.Rounded.Replay10,
            contentDescription = null,
          )
        }

        IconButton(
          onClick = {},
        ) {
          Icon(
            Icons.Rounded.PlayArrow,
            contentDescription = null,
          )
        }

        Spacer(Modifier.width(16.dp))
      }

      LinearProgressIndicator(
        progress = {
          session.currentTime.inWholeMilliseconds.toFloat() /
            session.duration.inWholeMilliseconds.toFloat()
        },
        modifier = Modifier
          .align(Alignment.BottomStart)
          .padding(
            horizontal = 12.dp,
          )
          .height(2.dp)
          .fillMaxWidth(),
      )
    }
  }
}

@Composable
private fun Thumbnail(
  imageUrl: String,
  contentDescription: String?,
  modifier: Modifier = Modifier,
) {
  val painter = rememberAsyncImagePainter(imageUrl)
  val shape = RoundedCornerShape(8.dp)
  Image(
    painter = painter,
    contentDescription = contentDescription,
    modifier = modifier
      .size(ThumbnailSize)
      .clip(shape)
      .border(1.dp, MaterialTheme.colorScheme.secondary, shape),
  )
}

private val ThumbnailSize = 56.dp

private const val SharedBounds = "bounds"
private const val SharedImage = "image"
