// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.management.ui.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.back.OverlayPriorityBackHandler
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Close
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.core.coroutines.LoadState
import app.campfire.core.di.UserScope
import app.campfire.management.api.screen.ItemEditorScreen
import campfire.features.management.ui.generated.resources.Res
import campfire.features.management.ui.generated.resources.action_close_editor
import campfire.features.management.ui.generated.resources.action_discard
import campfire.features.management.ui.generated.resources.action_save
import campfire.features.management.ui.generated.resources.dialog_discard_cancel
import campfire.features.management.ui.generated.resources.dialog_discard_confirm
import campfire.features.management.ui.generated.resources.dialog_discard_message
import campfire.features.management.ui.generated.resources.dialog_discard_title
import campfire.features.management.ui.generated.resources.editor_changes
import campfire.features.management.ui.generated.resources.editor_load_failed
import campfire.features.management.ui.generated.resources.editor_subtitle
import campfire.features.management.ui.generated.resources.editor_title
import com.slack.circuit.codegen.annotations.CircuitInject
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@CircuitInject(ItemEditorScreen::class, UserScope::class)
@Composable
fun ItemEditorUi(
  state: ItemEditorUiState,
  modifier: Modifier = Modifier,
) {
  val draft = state.draft
  val changes by remember(draft) { derivedStateOf { draft?.changes.orEmpty() } }
  val canSave by remember(draft, state.canEdit, state.isSaving) {
    derivedStateOf { draft != null && state.canEdit && !state.isSaving && changes.isNotEmpty() && draft.isValid }
  }

  // Leaving with changes asks first; nothing leaves while a save is on its way
  OverlayPriorityBackHandler(
    enabled = (changes.isNotEmpty() || state.isSaving) && !state.isConfirmingDiscard,
    onBack = { if (!state.isSaving) state.eventSink(ItemEditorUiEvent.Close) },
  )

  val snackbarHost = remember { SnackbarHostState() }
  LaunchedEffect(state.message) {
    val message = state.message ?: return@LaunchedEffect
    snackbarHost.showSnackbar(message)
    state.eventSink(ItemEditorUiEvent.ClearMessage)
  }

  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
      .onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        when {
          event.key == Key.S && (event.isCtrlPressed || event.isMetaPressed) -> {
            if (canSave) state.eventSink(ItemEditorUiEvent.Save)
            true
          }

          event.key == Key.Escape && !state.isSaving -> {
            state.eventSink(ItemEditorUiEvent.Close)
            true
          }

          else -> false
        }
      },
  ) {
    val isWide = maxWidth >= WideWidth
    Scaffold(
      topBar = {
        EditorTopBar(
          state = state,
          changeCount = changes.size,
          canSave = canSave,
          isWide = isWide,
        )
      },
      snackbarHost = { SnackbarHost(snackbarHost) },
    ) { padding ->
      when {
        draft != null -> DetailsForm(
          draft = draft,
          enabled = state.canEdit && !state.isSaving,
          canEdit = state.canEdit,
          canSeeExplicit = state.canSeeExplicit,
          isBehindServer = state.isBehindServer,
          onReload = { state.eventSink(ItemEditorUiEvent.ResetChanges) },
          onKeepMine = { state.eventSink(ItemEditorUiEvent.KeepChanges) },
          twoColumns = maxWidth >= TwoColumnWidth,
          // The keyboard's padding includes the navigation bar the scaffold's already does
          modifier = Modifier
            .padding(padding)
            .consumeWindowInsets(padding),
        )

        state.item is LoadState.Loading -> Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier.fillMaxSize().padding(padding),
        ) {
          CircularProgressIndicator()
        }

        else -> Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
        ) {
          Text(
            text = stringResource(Res.string.editor_load_failed),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }

  if (state.isConfirmingDiscard) {
    DiscardChangesDialog(
      onDiscard = { state.eventSink(ItemEditorUiEvent.ConfirmDiscard) },
      onKeepEditing = { state.eventSink(ItemEditorUiEvent.CancelDiscard) },
    )
  }
}

@Composable
private fun EditorTopBar(
  state: ItemEditorUiState,
  changeCount: Int,
  canSave: Boolean,
  isWide: Boolean,
  modifier: Modifier = Modifier,
) {
  TopAppBar(
    navigationIcon = {
      val closeLabel = stringResource(Res.string.action_close_editor)
      IconButtonTooltip(text = closeLabel) {
        IconButton(
          onClick = { state.eventSink(ItemEditorUiEvent.Close) },
          enabled = !state.isSaving,
          modifier = Modifier.testTag("editor_close"),
        ) {
          Icon(CampfireIcons.Rounded.Close, contentDescription = closeLabel)
        }
      }
    },
    title = {
      Column {
        Text(
          text = stringResource(Res.string.editor_title),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        val item = state.item.dataOrNull
        val title = item?.media?.metadata?.title
        if (title != null) {
          val author = item.media.metadata.authorName
          Text(
            text = if (author != null) stringResource(Res.string.editor_subtitle, title, author) else title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    },
    actions = {
      if (isWide && changeCount > 0) {
        Text(
          text = pluralStringResource(Res.plurals.editor_changes, changeCount, changeCount),
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(horizontal = 8.dp),
        )
        TextButton(
          onClick = { state.eventSink(ItemEditorUiEvent.ResetChanges) },
          enabled = !state.isSaving,
          modifier = Modifier.testTag("editor_discard"),
        ) {
          Text(stringResource(Res.string.action_discard))
        }
      }

      Button(
        onClick = { state.eventSink(ItemEditorUiEvent.Save) },
        enabled = canSave,
        modifier = Modifier
          .padding(start = 4.dp, end = 12.dp)
          .testTag("editor_save"),
      ) {
        if (state.isSaving) {
          CircularProgressIndicator(
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(16.dp),
          )
          Spacer(Modifier.width(8.dp))
        }
        Text(stringResource(Res.string.action_save))
        if (!isWide && changeCount > 0 && !state.isSaving) {
          Spacer(Modifier.width(8.dp))
          ChangeCount(changeCount)
        }
      }
    },
    modifier = modifier,
  )
}

/** How many details have changed, on the Save button where there's no room to spell it out */
@Composable
private fun ChangeCount(
  count: Int,
  modifier: Modifier = Modifier,
) {
  val description = pluralStringResource(Res.plurals.editor_changes, count, count)
  Surface(
    shape = CircleShape,
    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.22f),
    contentColor = MaterialTheme.colorScheme.onPrimary,
    // Read as "2 changes" rather than a bare number
    modifier = modifier.clearAndSetSemantics { contentDescription = description },
  ) {
    Text(
      text = count.toString(),
      style = MaterialTheme.typography.labelSmall,
      modifier = Modifier
        .padding(horizontal = 6.dp, vertical = 1.dp)
        .testTag("editor_change_count"),
    )
  }
}

@Composable
private fun DiscardChangesDialog(
  onDiscard: () -> Unit,
  onKeepEditing: () -> Unit,
  modifier: Modifier = Modifier,
) {
  AlertDialog(
    onDismissRequest = onKeepEditing,
    title = { Text(stringResource(Res.string.dialog_discard_title)) },
    text = { Text(stringResource(Res.string.dialog_discard_message)) },
    confirmButton = {
      TextButton(
        onClick = onDiscard,
        modifier = Modifier.testTag("discard_confirm"),
      ) {
        Text(
          text = stringResource(Res.string.dialog_discard_confirm),
          color = MaterialTheme.colorScheme.error,
        )
      }
    },
    dismissButton = {
      TextButton(
        onClick = onKeepEditing,
        modifier = Modifier.testTag("discard_cancel"),
      ) {
        Text(stringResource(Res.string.dialog_discard_cancel))
      }
    },
    modifier = modifier,
  )
}

/** Wide enough to spell out the change count and offer to discard beside Save */
private val WideWidth: Dp = 600.dp

/** Wide enough for the form's two columns */
private val TwoColumnWidth: Dp = 720.dp
