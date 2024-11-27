package app.campfire.sessions.ui.chapters

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.campfire.core.model.Chapter
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
    ) { models, overlayNavigator ->
      ChapterListBottomSheet(
        chapters = models,
        onChapterClicked = { chapter ->
          overlayNavigator.finish(ChapterResult.Selected(chapter))
        },
        modifier = Modifier.navigationBarsPadding(),
      )
    }
  )
}

@Composable
private fun ChapterListBottomSheet(
  chapters: List<Chapter>,
  onChapterClicked: (Chapter) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier,
  ) {
    Text(
      text = stringResource(Res.string.chapters_bottomsheet_title),
      style = MaterialTheme.typography.titleLarge,
      modifier = Modifier
        .padding(16.dp)
        .align(Alignment.CenterHorizontally),
    )
    chapters.forEach { chapter ->
      ListItem(
        headlineContent = { Text(chapter.title) },
        modifier = Modifier.clickable {
          onChapterClicked(chapter)
        },
        colors = ListItemDefaults.colors(
          containerColor = Color.Transparent,
        )
      )
    }
  }
}
