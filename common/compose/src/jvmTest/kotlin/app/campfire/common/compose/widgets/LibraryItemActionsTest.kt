// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.rightClick
import androidx.compose.ui.test.v2.runComposeUiTest
import app.campfire.common.compose.layout.ContentLayout
import app.campfire.common.compose.layout.LocalContentLayout
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.PodcastEpisode
import app.campfire.core.model.preview.libraryItem
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isCloseTo
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.single
import com.slack.circuit.overlay.ContentWithOverlays
import com.slack.circuit.overlay.OverlayHost
import com.slack.circuit.sharedelements.PreviewSharedElementTransitionLayout
import kotlin.test.Test

/** Which item cards offer the item's actions, and how a long press and a right click open them. */
@OptIn(ExperimentalTestApi::class, ExperimentalSharedTransitionApi::class)
class LibraryItemActionsTest {

  private val item = libraryItem()
  private val actions = RecordingLibraryItemActions()
  private var clicks = 0

  @Test
  fun `a long press on a card opens its actions as a sheet`() = runComposeUiTest {
    setCards(actions) { Card() }

    onNodeWithTag(CardTag).assert(hasLongClickAction)
    onNodeWithTag(CardTag).performTouchInput { longClick() }
    waitForIdle()

    assertThat(actions.shown).containsExactly(item.id to null)
    assertThat(clicks).isEqualTo(0)
  }

  @Test
  fun `a tap on a card still opens the item`() = runComposeUiTest {
    setCards(actions) { Card() }

    onNodeWithTag(CardTag).performClick()
    waitForIdle()

    assertThat(clicks).isEqualTo(1)
    assertThat(actions.shown).isEmpty()
  }

  @Test
  fun `a right click on a card opens its actions where it was clicked`() = runComposeUiTest {
    setCards(actions) { Card() }

    onNodeWithTag(CardTag).performMouseInput { rightClick() }
    waitForIdle()

    val center = onNodeWithTag(CardTag).fetchSemanticsNode().boundsInWindow.center
    val pointer = assertThat(actions.shown).single().transform { it.second }.isNotNull()
    pointer.transform { it.x }.isCloseTo(center.x, 1f)
    pointer.transform { it.y }.isCloseTo(center.y, 1f)
    assertThat(clicks).isEqualTo(0)
  }

  @Test
  fun `a card offers nothing when the account can't act on the item`() = runComposeUiTest {
    actions.allowed = false
    setCards(actions) { Card() }

    onNodeWithTag(CardTag).assert(hasNoLongClickAction)
    onNodeWithTag(CardTag).performTouchInput { longClick() }
    onNodeWithTag(CardTag).performMouseInput { rightClick() }
    waitForIdle()

    assertThat(actions.shown).isEmpty()
  }

  @Test
  fun `a card offers nothing where nothing provides actions`() = runComposeUiTest {
    setCards(actions = null) { Card() }

    onNodeWithTag(CardTag).assert(hasNoLongClickAction)
  }

  @Test
  fun `an episode's card offers nothing, as the actions are the podcast's`() = runComposeUiTest {
    setCards(actions) { Card(episode = episode) }

    onNodeWithTag(CardTag).assert(hasNoLongClickAction)
    onNodeWithTag(CardTag).performMouseInput { rightClick() }
    waitForIdle()

    assertThat(actions.shown).isEmpty()
  }

  @Test
  fun `a card whose taps are handled around it leaves its presses alone`() = runComposeUiTest {
    setCards(actions) {
      LibraryItemCard(item = item, modifier = Modifier.testTag(CardTag))
    }

    onNodeWithTag(CardTag).assert(hasNoLongClickAction)
  }

  @Test
  fun `a list row that offers actions opens them on a long press or a right click`() = runComposeUiTest {
    setCards(actions) { Row(offersActions = true) }

    onNodeWithTag(RowTag).assert(hasLongClickAction)
    onNodeWithTag(RowTag).performTouchInput { longClick() }
    onNodeWithTag(RowTag).performMouseInput { rightClick() }
    waitForIdle()

    assertThat(actions.shown.map { (_, pointer) -> pointer != null }).containsExactly(false, true)
    assertThat(clicks).isEqualTo(0)
  }

  @Test
  fun `a list row that offers actions still opens the item on a tap`() = runComposeUiTest {
    setCards(actions) { Row(offersActions = true) }

    onNodeWithTag(RowTag).performClick()
    waitForIdle()

    assertThat(clicks).isEqualTo(1)
  }

  @Test
  fun `a list row leaves its presses alone unless it offers actions`() = runComposeUiTest {
    setCards(actions) { Row(offersActions = false) }

    onNodeWithTag(RowTag).assert(hasNoLongClickAction)
    onNodeWithTag(RowTag).performMouseInput { rightClick() }
    waitForIdle()

    assertThat(actions.shown).isEmpty()
  }

  @Composable
  private fun Card(episode: PodcastEpisode? = null) {
    LibraryItemCard(
      item = item,
      episode = episode,
      onClick = { clicks++ },
      modifier = Modifier.testTag(CardTag),
    )
  }

  @Composable
  private fun Row(offersActions: Boolean) {
    LibraryItemListItem(
      libraryItem = item,
      onClick = { clicks++ },
      offersActions = offersActions,
      modifier = Modifier.testTag(RowTag),
    )
  }

  private fun ComposeUiTest.setCards(
    actions: LibraryItemActions?,
    content: @Composable () -> Unit,
  ) {
    setContent {
      CompositionLocalProvider(
        LocalLibraryItemActions provides actions,
        LocalContentLayout provides ContentLayout.Root,
      ) {
        ContentWithOverlays {
          PreviewSharedElementTransitionLayout {
            content()
          }
        }
      }
    }
  }

  private val episode = PodcastEpisode(
    id = "episode-1",
    libraryItemId = item.id,
    podcastId = item.media.id,
    title = "Episode",
    addedAtMillis = 0L,
    updatedAtMillis = 0L,
    durationInMillis = 0L,
    sizeInBytes = 0L,
  )
}

private class RecordingLibraryItemActions : LibraryItemActions {
  var allowed = true
  val shown = mutableListOf<Pair<String, Offset?>>()

  @Composable
  override fun canAct(item: LibraryItem): Boolean = allowed

  override suspend fun show(
    item: LibraryItem,
    pointer: Offset?,
    overlayHost: OverlayHost,
    snackbarHost: SnackbarHostState,
  ) {
    shown += item.id to pointer
  }
}

private val hasLongClickAction = SemanticsMatcher.keyIsDefined(SemanticsActions.OnLongClick)
private val hasNoLongClickAction = SemanticsMatcher.keyNotDefined(SemanticsActions.OnLongClick)

private const val CardTag = "card"
private const val RowTag = "row"
