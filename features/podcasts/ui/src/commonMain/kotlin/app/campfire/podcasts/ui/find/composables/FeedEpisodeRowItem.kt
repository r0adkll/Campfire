package app.campfire.podcasts.ui.find.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.DisabledAlpha
import app.campfire.common.compose.extensions.relativeDayLabel
import app.campfire.common.compose.extensions.thresholdReadoutFormat
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Download
import app.campfire.common.compose.icons.rounded.Downloading
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.core.extensions.asDate
import app.campfire.podcasts.api.RemotePodcastEpisode
import app.campfire.podcasts.ui.find.FeedEpisodeRow

@Composable
internal fun FeedEpisodeRowItem(
  row: FeedEpisodeRow,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  shape: Shape = MaterialTheme.shapes.medium,
) {
  val isInteractive = !row.isAlreadyDownloaded && !row.isQueued
  val effectiveChecked = isSelected || row.isAlreadyDownloaded || row.isQueued
  val containerColor = when {
    row.isAlreadyDownloaded -> CampfireTheme.colorScheme.successContainer
    row.isQueued -> MaterialTheme.colorScheme.surfaceContainerLowest
    isSelected -> MaterialTheme.colorScheme.secondaryContainer
    else -> MaterialTheme.colorScheme.surfaceContainer
  }
  val contentColor = when {
    row.isAlreadyDownloaded -> CampfireTheme.colorScheme.onSuccessContainer.copy(
      alpha = DisabledAlpha,
    )
    row.isQueued -> MaterialTheme.colorScheme.onSurfaceVariant.copy(
      alpha = DisabledAlpha,
    )
    isSelected -> MaterialTheme.colorScheme.onSecondaryContainer
    else -> MaterialTheme.colorScheme.onSurface
  }
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = shape,
    color = containerColor,
    onClick = onClick,
    enabled = isInteractive,
  ) {
    CompositionLocalProvider(
      LocalContentColor provides contentColor,
    ) {
      Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.padding(16.dp),
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = row.episode.title,
            style = MaterialTheme.typography.titleMediumEmphasized,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )

          Spacer(Modifier.height(4.dp))

          MetadataRow(episode = row.episode)

          val plain = row.episode.descriptionPlain ?: row.episode.description
          if (!plain.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))

            Text(
              text = plain,
              style = MaterialTheme.typography.bodySmall,
              maxLines = 3,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        Spacer(Modifier.width(16.dp))

        when {
          row.isAlreadyDownloaded -> {
            Box(
              modifier = modifier
                .align(Alignment.CenterVertically)
                .size(40.dp),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                CampfireIcons.Rounded.Download,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = CampfireTheme.colorScheme.onSuccessContainer,
              )
            }
          }
          row.isQueued -> {
            Box(
              modifier = modifier
                .align(Alignment.CenterVertically)
                .size(40.dp),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                CampfireIcons.Rounded.Downloading,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface,
              )
            }
          }
          else -> {
            Checkbox(
              checked = effectiveChecked,
              onCheckedChange = null,
              enabled = isInteractive,
              colors = CheckboxDefaults.colors(
                disabledCheckedColor = MaterialTheme.colorScheme.primary,
              ),
              modifier = Modifier
                .padding(horizontal = 8.dp)
                .align(Alignment.CenterVertically),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun MetadataRow(
  episode: RemotePodcastEpisode,
  modifier: Modifier = Modifier,
) {
  val dateLabel = episode.publishedAtMillis?.asDate()?.relativeDayLabel
  val parts = buildList {
    dateLabel?.let { add(it) }
    episode.duration?.thresholdReadoutFormat()?.let { add(it) }
    episode.episode?.takeIf { it.isNotBlank() }?.let { add("#$it") }
    episode.season?.takeIf { it.isNotBlank() }?.let { add("S$it") }
    episode.episodeType
      ?.takeIf { it.isNotBlank() && !it.equals("full", ignoreCase = true) }
      ?.let { add(it.replaceFirstChar { c -> c.uppercase() }) }
  }
  if (parts.isEmpty()) return
  Text(
    text = parts.joinToString(" · "),
    style = MaterialTheme.typography.labelMedium,
//    color = ,
    modifier = modifier,
  )
}
