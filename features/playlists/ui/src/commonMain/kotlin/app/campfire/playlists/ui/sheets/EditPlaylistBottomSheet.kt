package app.campfire.playlists.ui.sheets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAddCheck
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.ActionEvent
import app.campfire.analytics.events.Created
import app.campfire.analytics.events.Updated
import app.campfire.common.compose.di.rememberComponent
import app.campfire.core.di.UserScope
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.core.model.Playlist
import app.campfire.core.model.PlaylistId
import app.campfire.playlists.api.PlaylistsRepository
import campfire.features.playlists.ui.generated.resources.Res
import campfire.features.playlists.ui.generated.resources.create_playlist_bottomsheet_title
import campfire.features.playlists.ui.generated.resources.edit_playlist_bottomsheet_action_create
import campfire.features.playlists.ui.generated.resources.edit_playlist_bottomsheet_action_creating
import campfire.features.playlists.ui.generated.resources.edit_playlist_bottomsheet_action_update
import campfire.features.playlists.ui.generated.resources.edit_playlist_bottomsheet_action_updating
import campfire.features.playlists.ui.generated.resources.edit_playlist_bottomsheet_input_description_label
import campfire.features.playlists.ui.generated.resources.edit_playlist_bottomsheet_input_title_label
import campfire.features.playlists.ui.generated.resources.update_playlist_bottomsheet_title
import com.r0adkll.kimchi.annotations.ContributesTo
import com.slack.circuit.overlay.OverlayHost
import com.slack.circuitx.overlays.BottomSheetOverlay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

sealed interface EditPlaylistResult {
  data object None : EditPlaylistResult

  data class Success(
    val id: PlaylistId,
    val name: String,
    val description: String?,
  ) : EditPlaylistResult
}

sealed interface EditPlaylistModel {
  data object New : EditPlaylistModel
  data class Existing(
    val playlist: Playlist,
  ) : EditPlaylistModel

  val maybeName: String?
    get() = (this as? Existing)?.playlist?.name

  val maybeDescription: String?
    get() = (this as? Existing)?.playlist?.description
}

@ContributesTo(UserScope::class)
interface EditPlaylistsBottomSheetComponent {
  val playlistsRepository: PlaylistsRepository
}

suspend fun OverlayHost.showEditPlaylistBottomSheet(
  model: EditPlaylistModel = EditPlaylistModel.New,
): EditPlaylistResult {
  return show(
    BottomSheetOverlay<EditPlaylistModel, EditPlaylistResult>(
      model = model,
      onDismiss = { EditPlaylistResult.None },
      sheetShape = RoundedCornerShape(
        topStart = 32.dp,
        topEnd = 32.dp,
      ),
    ) { model, overlayNavigator ->
      SheetScaffold(
        title = {
          Text(
            when (model) {
              is EditPlaylistModel.Existing -> stringResource(Res.string.update_playlist_bottomsheet_title)
              EditPlaylistModel.New -> stringResource(Res.string.create_playlist_bottomsheet_title)
            },
          )
        },
      ) {
        EditPlaylistBottomSheet(
          model = model,
          onPlaylistEdited = { id, name, desc ->
            overlayNavigator.finish(EditPlaylistResult.Success(id, name, desc))
          },
        )
        Spacer(Modifier.height(16.dp))

        Spacer(
          Modifier
            .navigationBarsPadding()
            .imePadding(),
        )
      }
    },
  )
}

@Composable
internal fun SheetScaffold(
  title: @Composable () -> Unit,
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit,
) {
  Column(
    modifier = modifier,
  ) {
    Box(
      Modifier
        .padding(16.dp)
        .align(Alignment.CenterHorizontally),
    ) {
      ProvideTextStyle(
        MaterialTheme.typography.titleLarge.copy(
          fontWeight = FontWeight.SemiBold,
        ),
      ) {
        title()
      }
    }

    content()
  }
}

@Composable
private fun EditPlaylistBottomSheet(
  model: EditPlaylistModel,
  modifier: Modifier = Modifier,
  onPlaylistEdited: (PlaylistId, String, String?) -> Unit,
  component: EditPlaylistsBottomSheetComponent = rememberComponent(),
) {
  val scope = rememberCoroutineScope()
  var isCreating by remember { mutableStateOf(false) }

  var name by remember { mutableStateOf(TextFieldValue(model.maybeName ?: "")) }
  var description by remember { mutableStateOf(TextFieldValue(model.maybeDescription ?: "")) }
  val isValid = name.text.isNotBlank()

  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(
        horizontal = 16.dp,
      ),
  ) {
    OutlinedTextField(
      enabled = !isCreating,
      value = name,
      onValueChange = { name = it },
      label = { Text(stringResource(Res.string.edit_playlist_bottomsheet_input_title_label)) },
      modifier = Modifier
        .fillMaxWidth(),
    )

    Spacer(Modifier.height(8.dp))

    OutlinedTextField(
      enabled = !isCreating,
      value = description,
      onValueChange = { description = it },
      label = { Text(stringResource(Res.string.edit_playlist_bottomsheet_input_description_label)) },
      modifier = Modifier
        .fillMaxWidth(),
    )

    Spacer(Modifier.height(24.dp))

    Button(
      onClick = {
        isCreating = true
        val newName = name.text.trim()
        val newDescription = description.text.trim().takeIf { it.isNotBlank() }

        scope.launch {
          when (model) {
            is EditPlaylistModel.Existing -> {
              component.editPlaylist(newName, newDescription, model)
              onPlaylistEdited(model.playlist.id, newName, newDescription)
            }

            EditPlaylistModel.New -> {
              component.createPlaylist(newName, newDescription)
                .onSuccess {
                  onPlaylistEdited(it, newName, newDescription)
                }
                .onFailure {
                  // TODO: Should probably add some sort of Error UI here
                  bark(LogPriority.ERROR, throwable = it) {
                    "Failed to create new playlist!"
                  }
                }
            }
          }

          isCreating = false
        }
      },
      enabled = isValid && !isCreating,
      contentPadding = ButtonDefaults.TextButtonWithIconContentPadding,
      modifier = Modifier.fillMaxWidth(),
    ) {
      if (isCreating) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp))
      } else {
        val icon = when (model) {
          EditPlaylistModel.New -> Icons.AutoMirrored.Rounded.PlaylistAddCheck
          is EditPlaylistModel.Existing -> Icons.Rounded.Save
        }
        Icon(icon, contentDescription = null)
      }
      Spacer(Modifier.width(ButtonDefaults.IconSpacing))
      if (isCreating) {
        Text(
          when (model) {
            EditPlaylistModel.New -> stringResource(Res.string.edit_playlist_bottomsheet_action_creating)
            is EditPlaylistModel.Existing -> stringResource(Res.string.edit_playlist_bottomsheet_action_updating)
          },
        )
      } else {
        Text(
          when (model) {
            EditPlaylistModel.New -> stringResource(Res.string.edit_playlist_bottomsheet_action_create)
            is EditPlaylistModel.Existing -> stringResource(Res.string.edit_playlist_bottomsheet_action_update)
          },
        )
      }
    }
  }
}

suspend fun EditPlaylistsBottomSheetComponent.editPlaylist(
  newName: String,
  newDescription: String?,
  model: EditPlaylistModel.Existing,
) {
  Analytics.send(ActionEvent("playlist", Updated))
  playlistsRepository.updatePlaylist(
    playlistId = model.playlist.id,
    name = newName,
    description = newDescription,
    items = model.playlist.items.map { it.asMinified() },
  ).onSuccess {
    bark { "Playlist successfully updated!" }
  }.onFailure {
    bark(LogPriority.ERROR, throwable = it) { "Playlist failed to update" }
  }
}

suspend fun EditPlaylistsBottomSheetComponent.createPlaylist(
  newName: String,
  newDescription: String?,
): Result<PlaylistId> {
  Analytics.send(ActionEvent("playlist", Created))
  return playlistsRepository.createPlaylist(
    name = newName,
    description = newDescription,
  )
}
