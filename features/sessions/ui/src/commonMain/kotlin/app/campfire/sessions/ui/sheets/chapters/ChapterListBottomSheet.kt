package app.campfire.sessions.ui.sheets.chapters

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.extensions.clockFormat
import app.campfire.core.extensions.fluentIf
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

suspend fun OverlayHost.showChapterBottomSheet(
  chapters: List<Chapter>,
  currentChapter: Chapter? = null,
): ChapterResult {
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
        currentChapter = currentChapter,
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
  currentChapter: Chapter? = null,
  onChapterClicked: (Chapter) -> Unit,
  modifier: Modifier = Modifier,
  progressColor: Color = MaterialTheme.colorScheme.primaryContainer,
  selectedColor: Color = MaterialTheme.colorScheme.primary,
) {
  SessionSheetLayout(
    modifier = modifier,
    title = { Text(stringResource(Res.string.chapters_bottomsheet_title)) },
  ) {
    val lazyListState = rememberLazyListState(
      initialFirstVisibleItemIndex = chapters
        .indexOfFirst { it.id == currentChapter?.id }
        .takeIf { it != -1 } ?: 0,
    )

    LazyColumn(
      state = lazyListState,
    ) {
      items(
        items = chapters,
        key = { it.id },
      ) { chapter ->
        val isCurrentChapter = currentChapter?.id == chapter.id

        ListItem(
          headlineContent = {
            Text(
              text = chapter.title,
              fontWeight = if (isCurrentChapter) FontWeight.Bold else null,
            )
          },
          trailingContent = {
            Text(
              text = chapter.start.seconds.clockFormat(),
              style = MaterialTheme.typography.labelLarge,
              fontWeight = if (isCurrentChapter) FontWeight.Bold else null,
            )
          },
          modifier = Modifier
            .clickable {
              onChapterClicked(chapter)
            }
            .fluentIf(isCurrentChapter) {
              drawBehind {
                val width = size.width // * progress
                drawRect(
                  color = progressColor,
                  topLeft = Offset(-ProgressCornerRadius.toPx(), 0f),
                  size = size.copy(width = width + ProgressCornerRadius.toPx()),
                )

                drawRoundRect(
                  color = selectedColor,
                  topLeft = Offset(-IndicatorSize.toPx(), IndicatorPadding.toPx()),
                  size = Size(IndicatorSize.toPx() * 2f, size.height - (IndicatorPadding * 2).toPx()),
                  cornerRadius = CornerRadius(IndicatorSize.toPx()),
                )
              }
            },
          colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
          ),
        )
      }
    }
  }
}

private val IndicatorSize = 8.dp
private val IndicatorPadding = 0.dp
private val ProgressCornerRadius = 24.dp
