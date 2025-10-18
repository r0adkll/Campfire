package app.campfire.collections.ui.dialog

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.CircularProgressIndicator
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
import app.campfire.collections.api.ui.AddToCollectionDialog
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.analytics.Impression
import app.campfire.common.compose.layout.isSupportingPaneEnabled
import app.campfire.core.coroutines.LoadState
import app.campfire.core.di.UserScope
import app.campfire.core.model.Collection
import app.campfire.core.model.LibraryItem
import campfire.features.collections.ui.generated.resources.Res
import campfire.features.collections.ui.generated.resources.dialog_add_collection_action_create
import campfire.features.collections.ui.generated.resources.dialog_add_collection_action_dismiss
import campfire.features.collections.ui.generated.resources.dialog_add_collection_error_message
import campfire.features.collections.ui.generated.resources.dialog_add_collection_name_label
import campfire.features.collections.ui.generated.resources.dialog_add_collection_text
import campfire.features.collections.ui.generated.resources.dialog_add_collection_title
import coil3.compose.rememberAsyncImagePainter
import com.r0adkll.kimchi.annotations.ContributesBinding
import me.tatarka.inject.annotations.Inject
import org.jetbrains.compose.resources.stringResource

@ContributesBinding(UserScope::class)
@Inject
class AddToCollectionDialogImpl(
  private val presenterFactory: (LibraryItem, OnDismissListener) -> AddToCollectionDialogPresenter,
) : AddToCollectionDialog {

  @Composable
  override fun Content(
    item: LibraryItem,
    onDismiss: () -> Unit,
    modifier: Modifier,
  ) {
    Impression {
      ScreenViewEvent("AddToCollection", ScreenType.Dialog)
    }

    val presenter = remember(item, onDismiss) {
      presenterFactory(item, onDismiss)
    }

    val viewState = presenter.present()
    Content(
      viewState = viewState,
      item = item,
      onDismiss = onDismiss,
      modifier = modifier,
    )
  }

  @Composable
  private fun Content(
    viewState: AddToCollectionViewState,
    item: LibraryItem,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(),
  ) {
    BasicAlertDialog(
      onDismissRequest = onDismiss,
      modifier = modifier,
      properties = properties,
    ) {
      AlertDialogContent(
        buttons = {
          AlertDialogFlowRow(
            mainAxisSpacing = ButtonsMainAxisSpacing,
            crossAxisSpacing = ButtonsCrossAxisSpacing,
          ) {
            TextButton(onClick = onDismiss) {
              Text(stringResource(Res.string.dialog_add_collection_action_dismiss))
            }
          }
        },
        icon = null,
        title = {
          Text(stringResource(Res.string.dialog_add_collection_title))
        },
        text = {
          Text(
            buildAnnotatedString {
              append(stringResource(Res.string.dialog_add_collection_text))
              withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append("\"${item.media.metadata.title}\"")
              }
            },
          )
        },
        content = {
          CollectionsContent(
            viewState = viewState,
            collections = viewState.collections,
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
  private fun CollectionsContent(
    viewState: AddToCollectionViewState,
    collections: LoadState<out List<Collection>>,
    modifier: Modifier = Modifier,
  ) {
    when (collections) {
      is LoadState.Loaded<out List<Collection>> -> CollectionsLoadedState(
        viewState = viewState,
        collections = collections.data,
        modifier = modifier,
      )

      LoadState.Error -> CollectionsErrorState(modifier)
      LoadState.Loading -> CollectionsLoadingState(modifier)
    }
  }

  @Composable
  private fun CollectionsLoadedState(
    viewState: AddToCollectionViewState,
    collections: List<Collection>,
    modifier: Modifier = Modifier,
  ) {
    LazyColumn(
      modifier = modifier,
      contentPadding = PaddingValues(
        vertical = 8.dp,
      ),
    ) {
      items(
        items = collections,
        key = { it.id },
      ) { collection ->
        ListItem(
          headlineContent = {
            Text(
              text = collection.name,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          },
          supportingContent = {
            Text(
              text = collection.description ?: "",
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
          },
          leadingContent = {
            val painter = rememberAsyncImagePainter(collection.books.firstOrNull()?.media?.coverImageUrl)
            Image(
              painter = painter,
              contentDescription = null,
              contentScale = ContentScale.Crop,
              modifier = Modifier
                .clip(MaterialTheme.shapes.medium)
                .size(56.dp),
            )
          },
          colors = ListItemDefaults.colors(
            containerColor = AlertDialogDefaults.containerColor,
          ),
          modifier = Modifier.clickable {
            viewState.eventSink(AddToCollectionViewEvent.CollectionClicked(collection))
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
        NewCollectionListItem(
          onCreate = { collectionName ->
            viewState.eventSink(AddToCollectionViewEvent.CreateCollection(collectionName))
          },
        )
      }
    }
  }

  @Composable
  private fun NewCollectionListItem(
    onCreate: (String) -> Unit,
    modifier: Modifier = Modifier,
  ) {
    val windowSizeClass = LocalWindowSizeClass.current
    if (windowSizeClass.isSupportingPaneEnabled) {
      NewCollectionListItemHorizontal(onCreate, modifier)
    } else {
      NewCollectionListItemVertical(onCreate, modifier)
    }
  }

  @Composable
  private fun NewCollectionListItemHorizontal(
    onCreate: (String) -> Unit,
    modifier: Modifier = Modifier,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = modifier.padding(horizontal = 16.dp),
    ) {
      var collectionName by rememberSaveable { mutableStateOf("") }
      OutlinedTextField(
        value = collectionName,
        onValueChange = { collectionName = it },
        label = { Text(stringResource(Res.string.dialog_add_collection_name_label)) },
        modifier = Modifier.weight(1f),
      )

      Spacer(Modifier.width(8.dp))

      Button(
        onClick = { onCreate(collectionName) },
      ) {
        Text(stringResource(Res.string.dialog_add_collection_action_create))
      }
    }
  }

  @Composable
  private fun NewCollectionListItemVertical(
    onCreate: (String) -> Unit,
    modifier: Modifier = Modifier,
  ) {
    Column(
      modifier = modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
    ) {
      var collectionName by rememberSaveable { mutableStateOf("") }
      OutlinedTextField(
        value = collectionName,
        onValueChange = { collectionName = it },
        label = { Text(stringResource(Res.string.dialog_add_collection_name_label)) },
        modifier = Modifier.fillMaxWidth(),
      )

      Spacer(Modifier.height(8.dp))

      Button(
        onClick = { onCreate(collectionName) },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(stringResource(Res.string.dialog_add_collection_action_create))
      }
    }
  }

  @Composable
  private fun CollectionsLoadingState(
    modifier: Modifier = Modifier,
  ) {
    Box(
      modifier = modifier
        .height(128.dp),
      contentAlignment = Alignment.Center,
    ) {
      CircularProgressIndicator()
    }
  }

  @Composable
  private fun CollectionsErrorState(
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
        text = stringResource(Res.string.dialog_add_collection_error_message),
        style = MaterialTheme.typography.bodyMedium,
      )
    }
  }
}
