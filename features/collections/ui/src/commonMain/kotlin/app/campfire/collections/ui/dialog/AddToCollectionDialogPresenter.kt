package app.campfire.collections.ui.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import app.campfire.collections.api.CollectionsRepository
import app.campfire.core.coroutines.LoadState
import app.campfire.core.model.LibraryItem
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

typealias OnDismissListener = () -> Unit

@Inject
class AddToCollectionDialogPresenter(
  @Assisted private val libraryItem: LibraryItem,
  @Assisted private val onDismiss: OnDismissListener,
  private val collectionsRepository: CollectionsRepository,
) : Presenter<AddToCollectionViewState> {

  @Composable
  override fun present(): AddToCollectionViewState {
    val scope = rememberCoroutineScope()

    val collections by remember {
      collectionsRepository.observeAllCollections()
        .map { LoadState.Loaded(it) }
        .catch { LoadState.Error }
    }.collectAsState(LoadState.Loading)

    return AddToCollectionViewState(
      collections = collections,
      eventSink = { event ->
        when (event) {
          is AddToCollectionViewEvent.CollectionClicked -> {
            scope.launch {
              collectionsRepository.addToCollection(
                bookId = libraryItem.id,
                collectionId = event.collection.id,
              )

              onDismiss()
            }
          }

          is AddToCollectionViewEvent.CreateCollection -> {
            scope.launch {
              collectionsRepository.createCollection(
                name = event.collectionName,
                description = null,
                bookIds = listOf(libraryItem.id),
              )

              onDismiss()
            }
          }
        }
      },
    )
  }
}
