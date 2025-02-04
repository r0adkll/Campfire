package app.campfire.stats.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.extensions.thresholdReadoutFormat
import app.campfire.common.compose.extensions.timeAgo
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Android
import app.campfire.common.compose.icons.rounded.Desktop
import app.campfire.common.compose.icons.rounded.Web
import app.campfire.common.compose.icons.rounded.iOS
import app.campfire.common.compose.widgets.ItemImage
import app.campfire.core.extensions.seconds
import app.campfire.stats.ui.StatsUiModel

@Composable
internal fun RecentSessionListItem(
  model: StatsUiModel.RecentSession,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  ListItem(
    leadingContent = {
      ItemImage(
        imageUrl = model.session.coverImageUrl,
        contentDescription = model.session.mediaMetadata.title,
        modifier = Modifier
          .size(56.dp)
          .clip(RoundedCornerShape(12.dp)),
      )
    },
    headlineContent = {
      Text(model.session.mediaMetadata.title ?: "Unknown")
    },
    supportingContent = {
      Text(
        text = "${model.session.updatedAt.timeAgo} • ${model.session.timeListening.seconds.thresholdReadoutFormat()}",
      )
    },
    trailingContent = {
      val imageVector = when {
        model.session.deviceInfo.manufacturer != null ||
          model.session.deviceInfo.osName?.contains("android", true) == true
        -> CampfireIcons.Rounded.Android

        model.session.deviceInfo.osName?.contains("iOS", true) == true
        -> CampfireIcons.Rounded.iOS

        model.session.deviceInfo.browserName != null
        -> CampfireIcons.Rounded.Web

        else -> CampfireIcons.Rounded.Desktop
      }
      Box(
        modifier = Modifier.width(40.dp),
        contentAlignment = Alignment.Center,
      ) {
        Icon(imageVector, contentDescription = model.session.deviceInfo.clientName)
      }
    },
    modifier = modifier
      .clickable(onClick = onClick),
  )
}
