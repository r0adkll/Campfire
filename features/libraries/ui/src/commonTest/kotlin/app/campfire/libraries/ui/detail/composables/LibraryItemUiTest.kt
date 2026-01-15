package app.campfire.libraries.ui.detail.composables

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Density
import app.campfire.collections.api.ui.AddToCollectionDialog
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.layout.ContentLayout
import app.campfire.common.compose.layout.LocalContentLayout
import app.campfire.common.test.user
import app.campfire.core.coroutines.LoadState
import app.campfire.core.model.LibraryItem
import app.campfire.home.ui.libraryItem
import app.campfire.libraries.ui.detail.LibraryItem
import app.campfire.libraries.ui.detail.LibraryItemUiEvent
import app.campfire.libraries.ui.detail.LibraryItemUiState
import app.campfire.libraries.ui.detail.composables.slots.ContentSlot
import com.slack.circuit.sharedelements.SharedElementTransitionLayout
import com.slack.circuit.test.TestEventSink
import kotlin.test.Test

@OptIn(
  ExperimentalTestApi::class,
  ExperimentalSharedTransitionApi::class,
  ExperimentalMaterial3WindowSizeClassApi::class,
)
class LibraryItemUiTest {

  val user = user("test_user-id")
  private val events = TestEventSink<LibraryItemUiEvent>()
  private val addToCollectionDialog = object : AddToCollectionDialog {
    @Composable
    override fun Content(
      item: LibraryItem,
      onDismiss: () -> Unit,
      modifier: Modifier,
    ) {
      Box(
        modifier = modifier
          .testTag("add_to_collection_dialog"),
      )
    }
  }

  @Test
  fun testErrorList() = runComposeUiTest {
    val state = LibraryItemUiState(
      user = user,
      libraryItem = null,
      theme = null,
      swatch = null,
      contentState = LoadState.Error,
      showConfirmDownloadDialog = false,
      eventSink = events::invoke,
    )

    setContent {
      TestLibraryItem(state)
    }

    onNodeWithTag("error_list_state").assertIsDisplayed()
  }

  @Test
  fun testLoadingList() = runComposeUiTest {
    val state = LibraryItemUiState(
      user = user,
      libraryItem = null,
      theme = null,
      swatch = null,
      contentState = LoadState.Loading,
      showConfirmDownloadDialog = false,
      eventSink = events::invoke,
    )

    setContent {
      TestLibraryItem(state)
    }

    onNodeWithTag("loading_list_state").assertIsDisplayed()
  }

  @Test
  fun testLoadedList() = runComposeUiTest {
    val state = LibraryItemUiState(
      user = user,
      libraryItem = null,
      theme = null,
      swatch = null,
      contentState = LoadState.Loaded(
        data = List(20) { TestContentSlot("slot_$it") },
      ),
      showConfirmDownloadDialog = false,
      eventSink = events::invoke,
    )

    setContent {
      TestLibraryItem(state)
    }

    onNode(hasScrollAction())
      .onChildren()
      // Account for the spacer we add to the bottom of this list
      .assertCountEquals(21)
  }

  @Test
  fun clickingBackEmitsEvent() = runComposeUiTest {
    val state = LibraryItemUiState(
      user = user,
      libraryItem = null,
      theme = null,
      swatch = null,
      contentState = LoadState.Loaded(
        data = List(20) { TestContentSlot("slot_$it") },
      ),
      showConfirmDownloadDialog = false,
      eventSink = events::invoke,
    )

    setContent {
      TestLibraryItem(state)
    }

    onNodeWithContentDescription("Back").performClick()
    events.assertEvent(LibraryItemUiEvent.OnBack)
  }

  @Test
  fun clickingAddToCollectionShowsDialog() = runComposeUiTest {
    val state = LibraryItemUiState(
      user = user,
      libraryItem = libraryItem(),
      theme = null,
      swatch = null,
      contentState = LoadState.Loaded(
        data = List(20) { TestContentSlot("slot_$it") },
      ),
      showConfirmDownloadDialog = false,
      eventSink = events::invoke,
    )

    setContent {
      TestLibraryItem(state)
    }

    onNodeWithContentDescription("Add to collection").performClick()
    onNodeWithTag("add_to_collection_dialog").assertExists()
  }

  @Composable
  private fun TestLibraryItem(
    state: LibraryItemUiState,
    modifier: Modifier = Modifier,
  ) {
    SharedElementTransitionLayout {
      CompositionLocalProvider(
        LocalWindowSizeClass provides WindowSizeClass.calculateFromSize(Size(720f, 1080f), Density(1f)),
        LocalContentLayout provides ContentLayout.Root,
      ) {
        LibraryItem(
          state = state,
          addToCollectionDialog = addToCollectionDialog,
          modifier = modifier,
        )
      }
    }
  }
}

private class TestContentSlot(
  override val id: String,
) : ContentSlot {

  @Composable
  override fun Content(modifier: Modifier, eventSink: (LibraryItemUiEvent) -> Unit) {
    Box(modifier = modifier.testTag(id))
  }
}
