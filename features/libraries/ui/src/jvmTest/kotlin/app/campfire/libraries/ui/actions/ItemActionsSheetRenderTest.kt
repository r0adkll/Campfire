// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.actions

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.use
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.common.compose.widgets.sheets.LocalSheetPresentation
import app.campfire.common.compose.widgets.sheets.SheetPresentation
import app.campfire.core.model.LibraryItem
import app.campfire.home.ui.libraryItem
import app.campfire.home.ui.media
import app.campfire.home.ui.mediaMetadata
import app.campfire.libraries.ui.detail.manage.ItemAction
import assertk.assertThat
import assertk.assertions.isGreaterThan
import java.io.File
import kotlin.test.Test
import org.jetbrains.skia.EncodedImageFormat

/** An item's long-press sheet, as a phone and a short window show it. PNGs land in `build/renders`. */
@OptIn(ExperimentalComposeUiApi::class)
class ItemActionsSheetRenderTest {

  private val book = libraryItem(
    id = "book",
    isFile = false,
    media = media(
      metadata = mediaMetadata(title = "Children of Memory", authorName = "Adrian Tchaikovsky"),
      durationInMillis = (15 * 60 + 30) * 60_000L,
    ),
  )

  private val managing = listOf(
    ItemAction.Edit,
    ItemAction.Match,
    ItemAction.ChangeCover,
    ItemAction.QuickMatch,
    ItemAction.Rescan,
    ItemAction.Delete,
  )

  @Test
  fun phone() = render(
    "item-actions-sheet.png",
    width = 412,
    height = 915,
    item = book,
    quick = listOf(
      QuickAction.Play(resume = true),
      QuickAction.Download,
      QuickAction.MarkFinished,
      QuickAction.AddToQueue,
      QuickAction.AddToPlaylist,
      QuickAction.AddToCollection,
    ),
  )

  @Test
  fun phoneListener() = render(
    "item-actions-sheet-listener.png",
    width = 412,
    height = 915,
    item = book,
    actions = emptyList(),
    quick = listOf(
      QuickAction.Play(resume = false),
      QuickAction.RemoveDownload,
      QuickAction.MarkNotFinished,
      QuickAction.RemoveFromQueue,
      QuickAction.AddToPlaylist,
    ),
  )

  @Test
  fun sideSheet() = render(
    "item-actions-side-sheet.png",
    width = 915,
    height = 412,
    item = book,
    side = true,
    quick = listOf(
      QuickAction.Play(resume = false),
      QuickAction.Download,
      QuickAction.MarkFinished,
      QuickAction.AddToQueue,
      QuickAction.AddToPlaylist,
      QuickAction.AddToCollection,
    ),
  )

  private fun render(
    name: String,
    width: Int,
    height: Int,
    item: LibraryItem,
    quick: List<QuickAction>,
    actions: List<ItemAction> = managing,
    side: Boolean = false,
  ) {
    val renders = File("build/renders").apply { mkdirs() }
    val bytes = ImageComposeScene(
      width = width * 2,
      height = height * 2,
      density = Density(2f),
      content = { Sheet(item, quick, actions, side) },
    ).use { scene ->
      repeat(5) { frame -> scene.render(frame * 100_000_000L) }
      scene.render(nanoTime = 2_000_000_000L).encodeToData(EncodedImageFormat.PNG)!!.bytes
    }
    File(renders, name).writeBytes(bytes)
    assertThat(bytes.size).isGreaterThan(1_000)
  }

  @Composable
  private fun Sheet(item: LibraryItem, quick: List<QuickAction>, actions: List<ItemAction>, side: Boolean) {
    CampfireTheme(useDarkColors = false) {
      CompositionLocalProvider(
        LocalSheetPresentation provides if (side) SheetPresentation.Side else SheetPresentation.Bottom,
      ) {
        Box(Modifier.fillMaxSize(), contentAlignment = if (side) Alignment.CenterEnd else Alignment.BottomCenter) {
          // As the app's sheets draw them: rounded towards the content, with a handle on a bottom sheet
          Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = if (side) {
              RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp)
            } else {
              RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            },
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              if (!side) BottomSheetDefaults.DragHandle()
              ItemActionsSheet(
                item = item,
                quickActions = quick,
                actions = actions,
                onQuickAction = {},
                onAction = {},
              )
            }
          }
        }
      }
    }
  }
}
