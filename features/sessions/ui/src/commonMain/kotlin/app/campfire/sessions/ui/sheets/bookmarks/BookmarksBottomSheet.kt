package app.campfire.sessions.ui.sheets.bookmarks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Forward5
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.Bookmark as BookmarkObj
import app.campfire.analytics.events.Created
import app.campfire.analytics.events.Deleted
import app.campfire.analytics.events.PlaybackActionEvent
import app.campfire.analytics.events.ScreenType
import app.campfire.analytics.events.ScreenViewEvent
import app.campfire.audioplayer.AudioPlayerHolder
import app.campfire.common.compose.analytics.Impression
import app.campfire.common.compose.di.rememberComponent
import app.campfire.common.compose.extensions.readoutFormat
import app.campfire.common.compose.icons.rounded.Bookmark
import app.campfire.common.compose.icons.rounded.BookmarkStar
import app.campfire.common.compose.widgets.FilledTonalIconButton
import app.campfire.common.compose.widgets.SizedIcon
import app.campfire.common.compose.widgets.bottomSheetShape
import app.campfire.common.compose.widgets.circularReveal
import app.campfire.core.coroutines.LoadState
import app.campfire.core.coroutines.onError
import app.campfire.core.coroutines.onLoaded
import app.campfire.core.coroutines.onLoading
import app.campfire.core.di.UserScope
import app.campfire.core.model.Bookmark
import app.campfire.core.model.LibraryItemId
import app.campfire.sessions.ui.sheets.SessionSheetLayout
import app.campfire.user.api.BookmarkRepository
import campfire.features.sessions.ui.generated.resources.Res
import campfire.features.sessions.ui.generated.resources.bookmark_bottomsheet_action_create
import campfire.features.sessions.ui.generated.resources.bookmark_bottomsheet_empty_message
import campfire.features.sessions.ui.generated.resources.bookmark_bottomsheet_error_message
import campfire.features.sessions.ui.generated.resources.bookmark_bottomsheet_title
import campfire.features.sessions.ui.generated.resources.bookmark_delete_action_cancel
import campfire.features.sessions.ui.generated.resources.bookmark_delete_action_delete
import campfire.features.sessions.ui.generated.resources.bookmark_delete_title
import campfire.features.sessions.ui.generated.resources.bookmark_new_dialog_action_cancel
import campfire.features.sessions.ui.generated.resources.bookmark_new_dialog_action_create
import campfire.features.sessions.ui.generated.resources.bookmark_new_dialog_label_title
import campfire.features.sessions.ui.generated.resources.bookmark_new_dialog_title
import com.r0adkll.kimchi.annotations.ContributesTo
import com.slack.circuit.overlay.OverlayHost
import com.slack.circuitx.overlays.BottomSheetOverlay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

sealed interface BookmarkResult {
  data object None : BookmarkResult
  data class Selected(val bookmark: Bookmark) : BookmarkResult
}

@ContributesTo(UserScope::class)
interface BookmarksBottomSheetComponent {
  val audioPlayerHolder: AudioPlayerHolder
  val bookmarkRepository: BookmarkRepository
}

suspend fun OverlayHost.showBookmarksBottomSheet(libraryItemId: LibraryItemId): BookmarkResult {
  return show(
    BottomSheetOverlay<LibraryItemId, BookmarkResult>(
      model = libraryItemId,
      onDismiss = { BookmarkResult.None },
      sheetShape = bottomSheetShape,
      skipPartiallyExpandedState = true,
    ) { id, overlayNavigator ->
      Impression {
        ScreenViewEvent("Bookmarks", ScreenType.Overlay)
      }

      SessionSheetLayout(
        title = { Text(stringResource(Res.string.bookmark_bottomsheet_title)) },
      ) {
        BookmarksBottomSheet(
          libraryItemId = id,
          onBookmarkClick = { bookmark ->
            overlayNavigator.finish(BookmarkResult.Selected(bookmark))
          },
        )
        Spacer(
          Modifier.navigationBarsPadding(),
        )
      }
    },
  )
}

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
private fun BookmarksBottomSheet(
  libraryItemId: LibraryItemId,
  onBookmarkClick: (Bookmark) -> Unit,
  modifier: Modifier = Modifier,
  component: BookmarksBottomSheetComponent = rememberComponent(),
) {
  val scope = rememberCoroutineScope()

  val currentTime by remember {
    component.audioPlayerHolder.currentPlayer.filterNotNull().flatMapLatest { it.overallTime }
  }.collectAsState(0.seconds)

  val loadState by remember {
    component.bookmarkRepository.observeBookmarks(libraryItemId)
      .map { LoadState.Loaded(it) }
      .catch<LoadState<out List<Bookmark>>> { emit(LoadState.Error) }
  }.collectAsState(LoadState.Loading)

  var showCreateDialog by remember { mutableStateOf<Duration?>(null) }

  Box(
    modifier = modifier,
  ) {
    LazyColumn {
      loadState
        .onLoaded { bookmarks ->
          items(bookmarks) { bookmark ->
            BookmarkListItem(
              bookmark = bookmark,
              onClick = {
                onBookmarkClick(bookmark)
              },
              onDeleteClick = {
                scope.launch {
                  Analytics.send(PlaybackActionEvent(BookmarkObj, Deleted))
                  component.bookmarkRepository
                    .removeBookmark(bookmark.libraryItemId, bookmark.time)
                }
              },
              modifier = Modifier.animateItem(),
            )
          }

          if (bookmarks.isEmpty()) {
            item {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(100.dp),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = stringResource(Res.string.bookmark_bottomsheet_empty_message),
                  style = MaterialTheme.typography.bodyLarge,
                )
              }
            }
          }
        }
        .onLoading {
          item {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
              contentAlignment = Alignment.Center,
            ) {
              CircularProgressIndicator()
            }
          }
        }
        .onError {
          item {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(200.dp),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = stringResource(Res.string.bookmark_bottomsheet_error_message),
                textAlign = TextAlign.Center,
              )
            }
          }
        }

      item {
        CreateNewListItem(
          currentTime = currentTime,
          onClick = {
            showCreateDialog = currentTime
          },
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
      }
    }
  }

  if (showCreateDialog != null) {
    CreateNewDialog(
      currentTime = showCreateDialog!!,
      onDismiss = { showCreateDialog = null },
      onCreate = { title, timestamp ->
        scope.launch {
          Analytics.send(PlaybackActionEvent(BookmarkObj, Created))
          component.bookmarkRepository
            .createBookmark(
              libraryItemId = libraryItemId,
              timestamp = timestamp,
              title = title,
            )
        }
        showCreateDialog = null
      },
    )
  }
}

@Composable
private fun BookmarkListItem(
  bookmark: Bookmark,
  onClick: () -> Unit,
  onDeleteClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var isConfirmDeleteVisible by remember { mutableStateOf(false) }
  Box(
    modifier = modifier,
  ) {
    BookmarkListItemContent(
      bookmark = bookmark,
      onClick = onClick,
      onDeleteClick = {
        isConfirmDeleteVisible = true
      },
      modifier = Modifier.zIndex(
        if (isConfirmDeleteVisible) 0f else 1f,
      ),
    )

    Row(
      modifier = Modifier.zIndex(
        if (isConfirmDeleteVisible) 1f else 0f,
      ).circularReveal(
        isVisible = isConfirmDeleteVisible,
        revealFrom = Offset(1f, 0.5f),
      ).background(MaterialTheme.colorScheme.error).matchParentSize().padding(horizontal = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = stringResource(Res.string.bookmark_delete_title),
        modifier = Modifier.weight(1f),
        color = MaterialTheme.colorScheme.onError,
      )
      TextButton(
        onClick = { isConfirmDeleteVisible = false },
        colors = ButtonDefaults.textButtonColors(
          contentColor = MaterialTheme.colorScheme.onError,
        ),
      ) {
        Text(stringResource(Res.string.bookmark_delete_action_cancel))
      }
      FilledTonalIconButton(
        onClick = {
          onDeleteClick()
          isConfirmDeleteVisible = false
        },
        icon = {
          SizedIcon(
            Icons.Rounded.DeleteForever,
            contentDescription = null,
          )
        },
        label = { Text(stringResource(Res.string.bookmark_delete_action_delete)) },
      )
    }
  }
}

@Composable
private fun BookmarkListItemContent(
  bookmark: Bookmark,
  onClick: () -> Unit,
  onDeleteClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  ListItem(
    modifier = modifier.clickable(onClick = onClick),
    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    leadingContent = {
      Icon(
        Icons.Rounded.Bookmark,
        contentDescription = null,
      )
    },
    headlineContent = { Text(bookmark.title) },
    supportingContent = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          Icons.Rounded.Schedule,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(bookmark.time.readoutFormat())
      }
    },
    trailingContent = {
      IconButton(
        onClick = onDeleteClick,
      ) {
        Icon(
          Icons.Rounded.DeleteOutline,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.error,
        )
      }
    },
  )
}

@Composable
private fun CreateNewListItem(
  currentTime: Duration,
  onClick: (Duration) -> Unit,
  modifier: Modifier = Modifier,
) {
  Button(
    modifier = modifier.fillMaxWidth(),
    onClick = { onClick(currentTime) },
    contentPadding = ButtonDefaults.TextButtonWithIconContentPadding,
  ) {
    Icon(
      Icons.Rounded.BookmarkAdd,
      contentDescription = null,
      modifier = Modifier.size(ButtonDefaults.IconSize),
    )
    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
    Text(stringResource(Res.string.bookmark_bottomsheet_action_create, currentTime.readoutFormat()))
  }
}

@Composable
private fun CreateNewDialog(
  currentTime: Duration,
  onDismiss: () -> Unit,
  onCreate: (title: String, timestamp: Duration) -> Unit,
  modifier: Modifier = Modifier,
) {
  var bookmarkTime by remember { mutableStateOf(currentTime) }

  BasicAlertDialog(
    modifier = modifier,
    onDismissRequest = onDismiss,
    content = {
      Surface(
        modifier = Modifier.wrapContentWidth().wrapContentHeight(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 6.dp,
      ) {
        Column(
          modifier = Modifier.padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Icon(
            Icons.Rounded.BookmarkStar,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
          )

          Spacer(Modifier.height(16.dp))

          Text(
            text = stringResource(Res.string.bookmark_new_dialog_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
          )

          Spacer(Modifier.height(16.dp))

          Row(
            modifier = Modifier
              .wrapContentSize()
              .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(50),
              ),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            IconButton(
              onClick = {
                bookmarkTime -= 1.seconds
              },
              colors = IconButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colorScheme.primary,
              ),
            ) {
              Icon(
                Icons.Rounded.Replay,
                contentDescription = null,
              )
            }

            Text(
              text = bookmarkTime.readoutFormat(),
              style = MaterialTheme.typography.bodyLarge,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.padding(horizontal = 8.dp),
            )

            IconButton(
              onClick = {
                bookmarkTime += 5.seconds
              },
              colors = IconButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colorScheme.primary,
              ),
            ) {
              Icon(
                Icons.Rounded.Forward5,
                contentDescription = null,
              )
            }
          }

          Spacer(Modifier.height(16.dp))

          var titleValue by remember { mutableStateOf(TextFieldValue("")) }
          OutlinedTextField(
            value = titleValue,
            onValueChange = { newValue ->
              titleValue = newValue
            },
            label = { Text(stringResource(Res.string.bookmark_new_dialog_label_title)) },
            modifier = Modifier.fillMaxWidth(),
          )

          Spacer(Modifier.height(16.dp))

          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            modifier = Modifier.fillMaxWidth(),
          ) {
            TextButton(
              onClick = onDismiss,
            ) {
              Text(stringResource(Res.string.bookmark_new_dialog_action_cancel))
            }
            TextButton(
              enabled = titleValue.text.isNotBlank(),
              onClick = {
                onCreate(titleValue.text, bookmarkTime)
              },
            ) {
              Text(stringResource(Res.string.bookmark_new_dialog_action_create))
            }
          }
        }
      }
    },
  )
}
