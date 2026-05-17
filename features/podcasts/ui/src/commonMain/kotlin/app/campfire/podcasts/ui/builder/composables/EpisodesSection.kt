package app.campfire.podcasts.ui.builder.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.DeleteSweep
import app.campfire.common.compose.icons.rounded.SelectAll
import app.campfire.common.compose.widgets.EpisodeListItemDefaults
import app.campfire.common.compose.widgets.MetadataHeader
import app.campfire.podcasts.ui.builder.FeedState
import campfire.features.podcasts.ui.generated.resources.Res
import campfire.features.podcasts.ui.generated.resources.add_podcast_builder_episodes_clear
import campfire.features.podcasts.ui.generated.resources.add_podcast_builder_episodes_empty
import campfire.features.podcasts.ui.generated.resources.add_podcast_builder_episodes_error
import campfire.features.podcasts.ui.generated.resources.add_podcast_builder_episodes_hint
import campfire.features.podcasts.ui.generated.resources.add_podcast_builder_episodes_section_title
import campfire.features.podcasts.ui.generated.resources.add_podcast_builder_episodes_select_all
import org.jetbrains.compose.resources.stringResource

internal fun LazyListScope.episodesSection(
  feedState: FeedState,
  selectedUrls: Set<String>,
  onToggle: (String) -> Unit,
  onSelectAll: () -> Unit,
  onClear: () -> Unit,
  onRetry: () -> Unit,
) {
  item("episodes-header") {
    EpisodesSectionHeader(
      feedState = feedState,
      selectedCount = selectedUrls.size,
      onSelectAll = onSelectAll,
      onClear = onClear,
    )
  }
  when (feedState) {
    is FeedState.Loaded -> {
      if (feedState.episodes.isNotEmpty()) {
        items(
          count = feedState.episodes.size,
          key = { index -> feedState.episodes[index].enclosureUrl },
        ) { index ->
          val episode = feedState.episodes[index]
          val isFirst = index == 0
          val isLast = index == feedState.episodes.lastIndex
          EpisodeRow(
            episode = episode,
            isSelected = episode.enclosureUrl in selectedUrls,
            isFirst = isFirst,
            isLast = isLast,
            onToggle = { onToggle(episode.enclosureUrl) },
          )
        }
      } else {
        item {
          EpisodesMessageState {
            Text(
              text = stringResource(Res.string.add_podcast_builder_episodes_empty),
              fontStyle = FontStyle.Italic,
            )
          }
        }
      }
    }

    is FeedState.Loading -> {
      item {
        EpisodesLoadingState()
      }
    }

    is FeedState.Error -> {
      item {
        EpisodesMessageState {
          Text(
            text = stringResource(Res.string.add_podcast_builder_episodes_error),
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EpisodesSectionHeader(
  feedState: FeedState,
  selectedCount: Int,
  onSelectAll: () -> Unit,
  onClear: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = MaterialTheme.shapes.extraLarge.copy(
      bottomStart = ZeroCornerSize,
      bottomEnd = ZeroCornerSize,
    ),
    color = EpisodesContainerColor,
  ) {
    Column {
      Spacer(Modifier.height(8.dp))

      // Header
      MetadataHeader(
        title = {
          Row(
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(stringResource(Res.string.add_podcast_builder_episodes_section_title))

            if (feedState is FeedState.Loaded && feedState.episodes.isNotEmpty()) {
              Spacer(Modifier.width(8.dp))
              Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shadowElevation = 1.dp,
              ) {
                if (selectedCount == 0) {
                  Text(
                    text = feedState.episodes.size.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(
                      horizontal = 6.dp,
                      vertical = 4.dp,
                    ),
                  )
                } else {
                  Text(
                    text = buildAnnotatedString {
                      withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(selectedCount.toString())
                      }
                      append(" of ")
                      withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(feedState.episodes.size.toString())
                      }
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(
                      horizontal = 8.dp,
                      vertical = 6.dp,
                    ),
                  )
                }
              }
            }
          }
        },
        trailingContent = if (feedState is FeedState.Loaded) {
          {
            EpisodesActionGroup(
              selectedCount = selectedCount,
              onClear = onClear,
              onSelectAll = onSelectAll,
            )
          }
        } else {
          null
        },
        modifier = Modifier
          .heightIn(min = 48.dp)
          .padding(
            horizontal = 24.dp,
          ),
      )

      Spacer(Modifier.height(8.dp))

      // Loaded content state
      if (feedState is FeedState.Loaded) {
        // TODO: Extract this
        Column(
          Modifier.padding(horizontal = 24.dp),
        ) {
          Text(
            text = stringResource(Res.string.add_podcast_builder_episodes_hint),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Spacer(Modifier.height(16.dp))
      }
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EpisodesActionGroup(
  selectedCount: Int,
  onClear: () -> Unit,
  onSelectAll: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val selectAllLabel = stringResource(Res.string.add_podcast_builder_episodes_select_all)
  val clearLabel = stringResource(Res.string.add_podcast_builder_episodes_clear)
  ButtonGroup(
    overflowIndicator = {
    },
    modifier = modifier,
  ) {
    customItem(
      buttonGroupContent = {
        val interactionSource = remember { MutableInteractionSource() }
        AnimatedVisibility(
          visible = selectedCount > 0,
          enter = fadeIn(),
          exit = fadeOut(),
        ) {
          FilledTonalIconButton(
            onClick = onClear,
            shapes = IconButtonDefaults.shapes(
              shape = IconButtonDefaults.extraSmallSquareShape,
              pressedShape = CircleShape,
            ),
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .size(IconButtonDefaults.extraSmallContainerSize())
              .animateWidth(interactionSource),
            interactionSource = interactionSource,
          ) {
            Icon(
              CampfireIcons.Rounded.DeleteSweep,
              contentDescription = clearLabel,
              modifier = Modifier.size(
                IconButtonDefaults.extraSmallIconSize,
              ),
            )
          }
        }
      },
      menuContent = {
      },
    )

    customItem(
      buttonGroupContent = {
        val buttonSize = ButtonDefaults.ExtraSmallContainerHeight
        val interactionSource = remember { MutableInteractionSource() }
        Button(
          onClick = onSelectAll,
          shapes = ButtonDefaults.shapes(),
          contentPadding = ButtonDefaults.contentPaddingFor(buttonSize, hasStartIcon = true),
          modifier = Modifier
            .heightIn(buttonSize)
            .animateWidth(interactionSource),
          interactionSource = interactionSource,
        ) {
          Icon(
            CampfireIcons.Rounded.SelectAll,
            contentDescription = selectAllLabel,
            modifier = Modifier.size(ButtonDefaults.iconSizeFor(buttonSize)),
          )
          Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(buttonSize)))
          Text(
            text = selectAllLabel,
            style = ButtonDefaults.textStyleFor(buttonSize),
          )
        }
      },
      menuContent = {},
    )
  }
}

private val EpisodeBlankSize = 200.dp

@Composable
private fun EpisodesMessageState(
  modifier: Modifier = Modifier,
  text: @Composable () -> Unit,
) {
  EpisodesContainer(modifier) {
    ElevatedCard(
      modifier = Modifier.fillMaxWidth(),
      shape = EpisodeListItemDefaults.singleItemShape(),
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(EpisodeBlankSize)
          .padding(horizontal = 48.dp),
        contentAlignment = Alignment.Center,
      ) {
        ProvideTextStyle(
          MaterialTheme.typography.bodyMedium,
        ) {
          text()
        }
      }
    }

    Spacer(Modifier.height(48.dp))
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EpisodesLoadingState(
  modifier: Modifier = Modifier,
) {
  EpisodesContainer(modifier) {
    ElevatedCard(
      modifier = Modifier.fillMaxWidth(),
      shape = EpisodeListItemDefaults.singleItemShape(),
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(EpisodeBlankSize)
          .padding(horizontal = 48.dp),
        contentAlignment = Alignment.Center,
      ) {
        CircularWavyProgressIndicator(
          modifier = Modifier.size(56.dp),
        )
      }
    }

    Spacer(Modifier.height(48.dp))
  }
}
