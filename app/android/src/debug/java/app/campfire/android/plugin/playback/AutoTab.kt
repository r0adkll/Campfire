// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.android.plugin.playback

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaBrowser
import app.campfire.android.plugin.common.LoadingIndicator
import app.campfire.android.plugin.common.SectionHeader
import app.campfire.android.plugin.playback.icons.KeyboardArrowUp
import com.livewire.ui.actions.clickAction
import com.livewire.ui.graphics.CircleShape
import com.livewire.ui.graphics.RoundedCornerShape
import com.livewire.ui.layout.Alignment
import com.livewire.ui.layout.Column
import com.livewire.ui.layout.Row
import com.livewire.ui.modifier.LivewireModifier
import com.livewire.ui.modifier.clip
import com.livewire.ui.modifier.fillMaxSize
import com.livewire.ui.modifier.fillMaxWidth
import com.livewire.ui.modifier.padding
import com.livewire.ui.modifier.size
import com.livewire.ui.modifier.verticalScroll
import com.livewire.ui.theme.LivewireTheme
import com.livewire.ui.widget.Button
import com.livewire.ui.widget.ButtonShapes
import com.livewire.ui.widget.ButtonSize
import com.livewire.ui.widget.Chip
import com.livewire.ui.widget.ChipStyle
import com.livewire.ui.widget.Icon
import com.livewire.ui.widget.Image
import com.livewire.ui.widget.Surface
import com.livewire.ui.widget.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Browses the session's media library (the Android Auto browse tree) without a car
 * or DHU — via the plugin's own [MediaBrowser], so every request exercises the
 * session's real MediaLibrarySession.Callback browse path.
 */
@Composable
internal fun AutoTab(browser: MediaBrowser?) {
  Column(
    LivewireModifier
      .fillMaxSize()
      .verticalScroll()
      .padding(16.dp),
  ) {
    if (browser == null) {
      LoadingIndicator("Connecting to session…", LivewireModifier.fillMaxWidth().padding(24.dp))
      return@Column
    }

    // Breadcrumb stack of (mediaId, title); empty = root
    var path by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    var items by remember { mutableStateOf<List<MediaItem>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var resolveTarget by remember { mutableStateOf<String?>(null) }

    val parentId = path.lastOrNull()?.first

    LaunchedEffect(browser, parentId) {
      items = null
      error = null
      runCatching {
        withContext(Dispatchers.Main) {
          val rootId = parentId ?: browser.getLibraryRoot(null).awaitFuture().checkedValue().mediaId
          browser.getChildren(rootId, 0, 100, null).awaitFuture().checkedValue()
        }
      }
        .onSuccess { items = it }
        .onFailure { error = "${it::class.simpleName}: ${it.message}" }
    }

    SectionHeader("Browse tree")
    Row(verticalAlignment = Alignment.CenterVertically) {
      Button(
        action = clickAction {
          path = path.dropLast(1)
          resolveTarget = null
        },
        size = ButtonSize.ExtraSmall,
        shapes = ButtonShapes(
          shape = RoundedCornerShape(8.dp),
          pressedShape = CircleShape,
        ),
        modifier = LivewireModifier.padding(2.dp),
      ) {
        if (path.isNotEmpty()) {
          Icon(KeyboardArrowUp)
        }
        Text(if (path.isEmpty()) "Root" else "Up")
      }
      Text(
        text = "/" + path.joinToString("/") { it.second },
        modifier = LivewireModifier.padding(8.dp),
        style = LivewireTheme.typography.titleSmall,
      )
    }

    when {
      error != null -> Text("Failed to load children: $error", color = Color.Red)
      items == null -> LoadingIndicator(
        message = "Loading children…",
        modifier = LivewireModifier.fillMaxWidth().padding(24.dp),
      )
      items?.isEmpty() == true -> Text("No children for this node.", color = Color.Gray)
      else -> items.orEmpty().forEach { item ->
        BrowseItemRow(
          item = item,
          onClick = clickAction(key = "browse_${item.mediaId}") {
            resolveTarget = null
            if (item.mediaMetadata.isBrowsable == true) {
              path = path + (item.mediaId to (item.mediaMetadata.title?.toString() ?: item.mediaId))
            } else {
              resolveTarget = item.mediaId
            }
          },
        )
      }
    }

    val target = resolveTarget
    if (target != null) {
      SectionHeader("getItem result")
      var resolvedText by remember(target) { mutableStateOf<String?>(null) }
      LaunchedEffect(target) {
        resolvedText = runCatching {
          withContext(Dispatchers.Main) { browser.getItem(target).awaitFuture().checkedValue() }
        }
          .fold(
            onSuccess = { resolved ->
              "mediaId=${resolved.mediaId}, title=${resolved.mediaMetadata.title}, " +
                "playable=${resolved.mediaMetadata.isPlayable}, " +
                "mediaType=${resolved.mediaMetadata.mediaType}"
            },
            onFailure = { "getItem failed: ${it::class.simpleName}: ${it.message}" },
          )
      }
      val result = resolvedText
      if (result == null) {
        LoadingIndicator(
          message = "Resolving $target…",
          modifier = LivewireModifier.fillMaxWidth().padding(16.dp),
        )
      } else {
        Text(result)
      }
    }
  }
}

private fun <T> LibraryResult<T>.checkedValue(): T {
  check(resultCode == LibraryResult.RESULT_SUCCESS) { "LibraryResult resultCode=$resultCode" }
  return checkNotNull(value) { "LibraryResult succeeded but has no value" }
}

@Composable
private fun BrowseItemRow(
  item: MediaItem,
  onClick: com.livewire.ui.actions.ClickAction,
) {
  val browsable = item.mediaMetadata.isBrowsable == true

  Surface(
    modifier = LivewireModifier
      .fillMaxWidth()
      .padding(2.dp),
    shape = RoundedCornerShape(8.dp),
    tonalElevation = 1.dp,
    onClick = onClick,
  ) {
    Row(
      LivewireModifier
        .fillMaxWidth()
        .padding(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // Artwork thumbnail when the item carries embedded artwork bytes, icon otherwise
      val artworkData = item.mediaMetadata.artworkData
      if (artworkData != null) {
        Image(
          imageData = artworkData,
          contentDescription = null,
          modifier = LivewireModifier
            .size(40.dp)
            .clip(RoundedCornerShape(6.dp)),
        )
      } else {
        Icon(
          imageVector = if (browsable) Icons.Rounded.Folder else Icons.Rounded.PlayArrow,
          modifier = LivewireModifier.size(24.dp),
          tint = Color.Gray,
        )
      }

      Column(
        LivewireModifier
          .weight(1f)
          .padding(8.dp),
      ) {
        Text(
          text = item.mediaMetadata.title?.toString() ?: "—",
          style = LivewireTheme.typography.titleSmall,
        )
        Text(
          text = item.mediaId,
          style = LivewireTheme.typography.bodySmall,
          color = Color.Gray,
        )
      }

      // Surface the extras Android Auto keys off (content style, download status)
      item.mediaMetadata.extras?.let { extras ->
        extras.keySet().take(3).forEach { key ->
          Chip(
            label = "${key.substringAfterLast('.')}=${extras.get(key)}",
            action = clickAction(key = "extra_${item.mediaId}_$key") {},
            modifier = LivewireModifier.padding(1.dp),
            style = ChipStyle.Assist,
          )
        }
      }
    }
  }
}
