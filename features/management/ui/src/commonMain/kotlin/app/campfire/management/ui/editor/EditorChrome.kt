// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.management.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.EditSquare
import app.campfire.common.compose.icons.rounded.PhotoAlbum
import app.campfire.common.compose.icons.rounded.Search
import app.campfire.common.compose.layout.isWidthAtLeastLarge
import app.campfire.common.compose.widgets.CoverImage
import app.campfire.common.compose.widgets.KeyCap
import app.campfire.common.compose.widgets.ShortcutHint
import app.campfire.common.compose.widgets.coverImageShape
import app.campfire.common.compose.widgets.placeholderBookPainter
import app.campfire.common.compose.widgets.primaryShortcut
import app.campfire.core.Platform
import app.campfire.core.currentPlatform
import app.campfire.core.model.LibraryItem
import campfire.features.management.ui.generated.resources.Res
import campfire.features.management.ui.generated.resources.action_discard
import campfire.features.management.ui.generated.resources.action_save
import campfire.features.management.ui.generated.resources.action_save_close
import campfire.features.management.ui.generated.resources.cd_changed
import campfire.features.management.ui.generated.resources.cd_editor_sections
import campfire.features.management.ui.generated.resources.editor_changes
import campfire.features.management.ui.generated.resources.shortcut_close
import campfire.features.management.ui.generated.resources.shortcut_save
import campfire.features.management.ui.generated.resources.tab_cover
import campfire.features.management.ui.generated.resources.tab_details
import campfire.features.management.ui.generated.resources.tab_match
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** How the editor lays itself out for the window it's in */
internal enum class EditorLayout {
  /** Tabs under the top bar, which holds the actions */
  Tabs,

  /** The sections down the side under the book; the top bar holds the actions */
  Rail,

  /** As [Rail], with the actions in a bar along the foot beside the keyboard's shortcuts for them */
  Desktop,
  ;

  val hasRail: Boolean
    get() = this != Tabs

  companion object {
    /**
     * The layout for [windowSizeClass]: the sections go down the side where the window has the room
     * for them beside the form and the preview, as a tablet on its side or a desktop does.
     */
    fun of(windowSizeClass: WindowSizeClass, platform: Platform = currentPlatform): EditorLayout = when {
      !windowSizeClass.isWidthAtLeastLarge -> Tabs
      platform == Platform.DESKTOP -> Desktop
      else -> Rail
    }
  }
}

internal val EditorSection.label: StringResource
  get() = when (this) {
    EditorSection.Details -> Res.string.tab_details
    EditorSection.Cover -> Res.string.tab_cover
    EditorSection.Match -> Res.string.tab_match
  }

private val EditorSection.icon: ImageVector
  get() = when (this) {
    EditorSection.Details -> CampfireIcons.Rounded.EditSquare
    EditorSection.Cover -> CampfireIcons.Rounded.PhotoAlbum
    EditorSection.Match -> CampfireIcons.Rounded.Search
  }

/** Whether any of [changes] were made in [this] section */
internal fun EditorSection.hasChanges(changes: Set<BookDetail>): Boolean = when (this) {
  EditorSection.Details -> changes.any { it != BookDetail.Cover }
  EditorSection.Cover -> BookDetail.Cover in changes
  // What a match fills in, it fills in on the form
  EditorSection.Match -> false
}

/**
 * The book being edited, then the editor's sections to move between, each marked once it has
 * changes and, with [showShortcuts], beside the keys that move to it.
 */
@Composable
internal fun SectionRail(
  item: LibraryItem?,
  section: EditorSection,
  sections: List<EditorSection>,
  changes: Set<BookDetail>,
  showShortcuts: Boolean,
  onSelect: (EditorSection) -> Unit,
  modifier: Modifier = Modifier,
) {
  val sectionsLabel = stringResource(Res.string.cd_editor_sections)
  Column(
    verticalArrangement = Arrangement.spacedBy(4.dp),
    modifier = modifier
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 12.dp, vertical = 8.dp)
      .semantics { paneTitle = sectionsLabel }
      .testTag("editor_sections"),
  ) {
    if (item != null) {
      BookHeader(
        item = item,
        modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 12.dp),
      )
    }
    sections.forEachIndexed { index, entry ->
      NavigationDrawerItem(
        label = {
          Text(
            text = stringResource(entry.label),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        },
        selected = entry == section,
        onClick = { onSelect(entry) },
        icon = { Icon(entry.icon, contentDescription = null) },
        badge = {
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            if (entry.hasChanges(changes)) ChangedDot()
            if (showShortcuts) KeyCap(primaryShortcut("${index + 1}"))
          }
        },
        modifier = Modifier
          .height(SectionItemHeight)
          .testTag("section_${entry.name}"),
      )
    }
  }
}

/** The book's cover, title and author, for which book's being edited */
@Composable
private fun BookHeader(
  item: LibraryItem,
  modifier: Modifier = Modifier,
) {
  Row(
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier.testTag("editor_book"),
  ) {
    CoverImage(
      imageUrl = item.media.takeIf { it.coverPath != null }?.coverImageUrl,
      contentDescription = null,
      size = HeaderCoverSize,
      placeholder = placeholderBookPainter(),
      shape = coverImageShape(HeaderCoverSize),
      modifier = Modifier.size(HeaderCoverSize),
    )
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = item.media.metadata.title.orEmpty(),
        style = MaterialTheme.typography.titleSmall,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      item.media.metadata.authorName?.let { author ->
        Text(
          text = author,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
private fun ChangedDot(modifier: Modifier = Modifier) {
  val changedLabel = stringResource(Res.string.cd_changed)
  Box(
    modifier
      .size(8.dp)
      .background(MaterialTheme.colorScheme.primary, CircleShape)
      .semantics { contentDescription = changedLabel },
  )
}

/**
 * The actions along the editor's foot on a desktop, beside the keys for them: throwing the changes
 * away, saving them and staying, or saving them and leaving.
 */
@Composable
internal fun EditorFooter(
  changeCount: Int,
  canSave: Boolean,
  isSaving: Boolean,
  onDiscard: () -> Unit,
  onSave: () -> Unit,
  onSaveAndClose: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    modifier = modifier.testTag("editor_footer"),
  ) {
    Column {
      HorizontalDivider()
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .navigationBarsPadding()
          .padding(start = 24.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
      ) {
        ShortcutHint(key = primaryShortcut("S"), action = stringResource(Res.string.shortcut_save))
        Spacer(Modifier.width(8.dp))
        ShortcutHint(key = "Esc", action = stringResource(Res.string.shortcut_close))
        Spacer(Modifier.weight(1f))

        if (isSaving) {
          CircularProgressIndicator(
            strokeWidth = 2.dp,
            modifier = Modifier
              .padding(horizontal = 8.dp)
              .size(16.dp)
              .testTag("editor_saving"),
          )
        } else if (changeCount > 0) {
          Text(
            text = pluralStringResource(Res.plurals.editor_changes, changeCount, changeCount),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp),
          )
        }

        val focusManager = LocalFocusManager.current
        TextButton(
          onClick = {
            // The fields are about to be replaced, so the keyboard lets go of the one it's in
            focusManager.clearFocus()
            onDiscard()
          },
          enabled = changeCount > 0 && !isSaving,
          modifier = Modifier.testTag("editor_discard"),
        ) {
          Text(stringResource(Res.string.action_discard))
        }
        FilledTonalButton(
          onClick = onSave,
          enabled = canSave,
          modifier = Modifier.testTag("editor_save_stay"),
        ) {
          Text(stringResource(Res.string.action_save))
        }
        Button(
          onClick = onSaveAndClose,
          enabled = canSave,
          modifier = Modifier.testTag("editor_save"),
        ) {
          Text(stringResource(Res.string.action_save_close))
        }
      }
    }
  }
}

/** How wide the sections are beside the form */
internal val SectionRailWidth: Dp = 232.dp

private val SectionItemHeight = 52.dp
private val HeaderCoverSize = 48.dp
