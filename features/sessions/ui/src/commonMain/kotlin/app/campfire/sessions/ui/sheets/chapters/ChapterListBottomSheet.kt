package app.campfire.sessions.ui.sheets.chapters

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.extensions.readoutFormat
import app.campfire.core.extensions.seconds
import app.campfire.core.model.Chapter
import app.campfire.sessions.ui.sheets.SessionSheetLayout
import campfire.features.sessions.ui.generated.resources.Res
import campfire.features.sessions.ui.generated.resources.chapters_bottomsheet_title
import com.slack.circuit.overlay.OverlayHost
import com.slack.circuitx.overlays.BottomSheetOverlay
import org.jetbrains.compose.resources.stringResource

sealed interface ChapterResult {
  data object None : ChapterResult
  data class Selected(val chapter: Chapter) : ChapterResult
}

suspend fun OverlayHost.showChapterBottomSheet(chapters: List<Chapter>): ChapterResult {
  return show(
    BottomSheetOverlay<List<Chapter>, ChapterResult>(
      model = chapters,
      onDismiss = { ChapterResult.None },
      sheetShape = RoundedCornerShape(
        topStart = 32.dp,
        topEnd = 32.dp,
      ),
    ) { models, overlayNavigator ->
      ChapterListBottomSheet(
        chapters = models,
        onChapterClicked = { chapter ->
          overlayNavigator.finish(ChapterResult.Selected(chapter))
        },
        modifier = Modifier.navigationBarsPadding(),
      )
    },
  )
}

@Composable
private fun ChapterListBottomSheet(
  chapters: List<Chapter>,
  onChapterClicked: (Chapter) -> Unit,
  modifier: Modifier = Modifier,
) {
  SessionSheetLayout(
    modifier = modifier,
    title = { Text(stringResource(Res.string.chapters_bottomsheet_title)) },
  ) {
    LazyColumn {
      items(
        items = chapters,
        key = { it.id },
      ) { chapter ->
        ListItem(
          headlineContent = { Text(chapter.title) },
          trailingContent = { Text(chapter.start.seconds.readoutFormat()) },
          modifier = Modifier.clickable {
            onChapterClicked(chapter)
          },
          colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
          ),
        )
      }
    }
  }
}
