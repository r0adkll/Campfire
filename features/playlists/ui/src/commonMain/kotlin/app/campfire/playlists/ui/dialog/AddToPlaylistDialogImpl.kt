package app.campfire.playlists.ui.dialog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import app.campfire.analytics.events.ScreenType
import app.campfire.analytics.events.ScreenViewEvent
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.analytics.Impression
import app.campfire.common.compose.layout.isSupportingPaneEnabled
import app.campfire.common.compose.widgets.AlertDialogContent
import app.campfire.common.compose.widgets.AlertDialogFlowRow
import app.campfire.common.compose.widgets.ButtonsCrossAxisSpacing
import app.campfire.common.compose.widgets.ButtonsMainAxisSpacing
import app.campfire.core.coroutines.LoadState
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Playlist
import app.campfire.playlists.api.dialog.AddToPlaylistDialog
import app.campfire.playlists.api.dialog.PlaylistDialogResult
import campfire.features.playlists.ui.generated.resources.Res
import campfire.features.playlists.ui.generated.resources.dialog_add_playlist_action_create
import campfire.features.playlists.ui.generated.resources.dialog_add_playlist_action_dismiss
import campfire.features.playlists.ui.generated.resources.dialog_add_playlist_error_message
import campfire.features.playlists.ui.generated.resources.dialog_add_playlist_name_label
import campfire.features.playlists.ui.generated.resources.dialog_add_playlist_text
import campfire.features.playlists.ui.generated.resources.dialog_add_playlist_title
import coil3.compose.rememberAsyncImagePainter
import com.r0adkll.kimchi.annotations.ContributesBinding
import me.tatarka.inject.annotations.Inject
import org.jetbrains.compose.resources.stringResource

@ContributesBinding(UserScope::class)
@Inject
class AddToPlaylistDialogImpl(
  private val presenterFactory: (LibraryItem, OnDismissListener) -> AddToPlaylistDialogPresenter,
) : AddToPlaylistDialog {

  @Composable
  override fun Content(
    libraryItem: LibraryItem,
    onDismiss: (PlaylistDialogResult) -> Unit,
    modifier: Modifier,
  ) {
    Impression {
      ScreenViewEvent("AddToPlaylist", ScreenType.Dialog)
    }

    val presenter = remember(libraryItem, onDismiss) {
      presenterFactory(libraryItem, onDismiss)
    }

    val viewState = presenter.present()
    Content(
      viewState = viewState,
      item = libraryItem,
      onDismiss = { onDismiss(PlaylistDialogResult.None) },
      modifier = modifier,
    )
  }

  @Composable
  private fun Content(
    viewState: AddToPlaylistViewState,
    item: LibraryItem,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(
      usePlatformDefaultWidth = false,
    ),
  ) {
    BasicAlertDialog(
      onDismissRequest = onDismiss,
      modifier = modifier
        .padding(horizontal = 24.dp),
      properties = properties,
    ) {
      AlertDialogContent(
        buttons = {
          AlertDialogFlowRow(
            mainAxisSpacing = ButtonsMainAxisSpacing,
            crossAxisSpacing = ButtonsCrossAxisSpacing,
          ) {
            TextButton(onClick = onDismiss) {
              Text(stringResource(Res.string.dialog_add_playlist_action_dismiss))
            }
          }
        },
        icon = null,
        title = {
          Text(stringResource(Res.string.dialog_add_playlist_title))
        },
        text = {
          Text(
            buildAnnotatedString {
              append(stringResource(Res.string.dialog_add_playlist_text))
              withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append("\"${item.media.metadata.title}\"")
              }
            },
          )
        },
        content = {
          PlaylistsContent(
            viewState = viewState,
          )
        },
        shape = AlertDialogDefaults.shape,
        containerColor = AlertDialogDefaults.containerColor,
        tonalElevation = AlertDialogDefaults.TonalElevation,
        iconContentColor = AlertDialogDefaults.iconContentColor,
        titleContentColor = AlertDialogDefaults.titleContentColor,
        textContentColor = AlertDialogDefaults.textContentColor,
        buttonContentColor = MaterialTheme.colorScheme.primary,
      )
    }
  }

  @Composable
  private fun PlaylistsContent(
    viewState: AddToPlaylistViewState,
    modifier: Modifier = Modifier,
  ) {
    when (val playlistState = viewState.playlists) {
      is LoadState.Loaded<out List<Playlist>> -> PlaylistsLoadedState(
        viewState = viewState,
        playlists = playlistState.data,
        modifier = modifier,
      )

      LoadState.Error -> PlaylistsErrorState(modifier)
      LoadState.Loading -> PlaylistsLoadingState(modifier)
    }
  }

  @OptIn(ExperimentalMaterial3ExpressiveApi::class)
  @Composable
  private fun PlaylistsLoadedState(
    viewState: AddToPlaylistViewState,
    playlists: List<Playlist>,
    modifier: Modifier = Modifier,
  ) {
    LazyColumn(
      modifier = modifier,
      contentPadding = PaddingValues(
        vertical = 8.dp,
      ),
    ) {
      items(
        items = playlists,
        key = { it.id },
      ) { playlist ->
        ListItem(
          headlineContent = {
            Text(
              text = playlist.name,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          },
          supportingContent = playlist.description?.let { desc ->
            {
              Text(
                text = desc,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )
            }
          },
          leadingContent = {
            Box(
              modifier = Modifier
                .clip(MaterialTheme.shapes.medium)
                .size(56.dp),
              contentAlignment = Alignment.Center,
            ) {
              val painter = rememberAsyncImagePainter(
                playlist.items.firstOrNull()?.libraryItem?.media?.coverImageUrl,
              )
              Image(
                painter = painter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
              )

              androidx.compose.animation.AnimatedVisibility(
                visible = (viewState.addLoadingState as? AddLoadingState.Playlist)
                  ?.playlistId == playlist.id,
                modifier = Modifier.fillMaxSize(),
              ) {
                Box(
                  modifier = Modifier
                    .fillMaxSize()
                    .background(
                      color = MaterialTheme.colorScheme.scrim.copy(0.5f),
                    ),
                  contentAlignment = Alignment.Center,
                ) {
                  CircularWavyProgressIndicator(
                    modifier = Modifier.size(32.dp),
                  )
                }
              }
            }
          },
          colors = ListItemDefaults.colors(
            containerColor = AlertDialogDefaults.containerColor,
          ),
          modifier = Modifier
            .clickable(
              enabled = viewState.addLoadingState == AddLoadingState.None ||
                viewState.addLoadingState == AddLoadingState.Error,
            ) {
              viewState.eventSink(AddToPlaylistViewEvent.PlaylistClicked(playlist))
            },
        )
      }

      item {
        HorizontalDivider(
          modifier = Modifier
            .fillMaxWidth()
            .padding(
              horizontal = 16.dp,
              vertical = 8.dp,
            ),
        )
      }

      item {
        NewPlaylistListItem(
          isLoading = viewState.addLoadingState is AddLoadingState.New,
          onCreate = { playlistName ->
            viewState.eventSink(AddToPlaylistViewEvent.CreatePlaylist(playlistName))
          },
        )
      }
    }
  }

  @Composable
  private fun NewPlaylistListItem(
    isLoading: Boolean,
    onCreate: (String) -> Unit,
    modifier: Modifier = Modifier,
  ) {
    val windowSizeClass = LocalWindowSizeClass.current
    if (windowSizeClass.isSupportingPaneEnabled) {
      NewPlaylistListItemHorizontal(isLoading, onCreate, modifier)
    } else {
      NewPlaylistListItemVertical(isLoading, onCreate, modifier)
    }
  }

  @Composable
  private fun NewPlaylistListItemHorizontal(
    isLoading: Boolean,
    onCreate: (String) -> Unit,
    modifier: Modifier = Modifier,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = modifier.padding(horizontal = 16.dp),
    ) {
      var playlistName by rememberSaveable { mutableStateOf("") }
      OutlinedTextField(
        enabled = !isLoading,
        value = playlistName,
        onValueChange = { playlistName = it },
        label = { Text(stringResource(Res.string.dialog_add_playlist_name_label)) },
        modifier = Modifier.weight(1f),
      )

      Spacer(Modifier.width(8.dp))

      Button(
        enabled = !isLoading,
        onClick = { onCreate(playlistName) },
      ) {
        AnimatedVisibility(
          visible = isLoading,
        ) {
          Row {
            CircularProgressIndicator(Modifier.size(18.dp))
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
          }
        }
        Text(stringResource(Res.string.dialog_add_playlist_action_create))
      }
    }
  }

  @Composable
  private fun NewPlaylistListItemVertical(
    isLoading: Boolean,
    onCreate: (String) -> Unit,
    modifier: Modifier = Modifier,
  ) {
    Column(
      modifier = modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
    ) {
      var playlistName by rememberSaveable { mutableStateOf("") }
      OutlinedTextField(
        enabled = !isLoading,
        value = playlistName,
        onValueChange = { playlistName = it },
        label = { Text(stringResource(Res.string.dialog_add_playlist_name_label)) },
        modifier = Modifier.fillMaxWidth(),
      )

      Spacer(Modifier.height(8.dp))

      Button(
        enabled = !isLoading,
        onClick = { onCreate(playlistName) },
        modifier = Modifier.fillMaxWidth(),
      ) {
        AnimatedVisibility(
          visible = isLoading,
        ) {
          Row {
            CircularProgressIndicator(Modifier.size(18.dp))
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
          }
        }
        Text(stringResource(Res.string.dialog_add_playlist_action_create))
      }
    }
  }

  @Composable
  private fun PlaylistsLoadingState(
    modifier: Modifier = Modifier,
  ) {
    Box(
      modifier = modifier
        .fillMaxWidth()
        .height(128.dp),
      contentAlignment = Alignment.Center,
    ) {
      CircularProgressIndicator()
    }
  }

  @Composable
  private fun PlaylistsErrorState(
    modifier: Modifier = Modifier,
  ) {
    Column(
      modifier = modifier
        .height(200.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Icon(
        Icons.Rounded.Error,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.error,
      )

      Text(
        text = stringResource(Res.string.dialog_add_playlist_error_message),
        style = MaterialTheme.typography.bodyMedium,
      )
    }
  }
}
