package app.campfire.stats.ui.composables

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.extensions.ReadoutStyle
import app.campfire.common.compose.extensions.readoutAtMostHours
import app.campfire.core.extensions.asReadableBytes
import app.campfire.stats.ui.StatsUiModel
import campfire.features.stats.ui.generated.resources.Res
import campfire.features.stats.ui.generated.resources.card_library_totals_title
import campfire.features.stats.ui.generated.resources.library_totals_summary_format
import kotlin.time.Duration
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LibraryTotalStatsCard(
  totals: StatsUiModel.LibraryTotals,
  modifier: Modifier = Modifier,
) {
  ElevatedCard(
    modifier = modifier
      .padding(
        horizontal = StatsDefaults.HorizontalPadding,
        vertical = StatsDefaults.VerticalPadding,
      )
      .fillMaxWidth(),
  ) {
    CardHeader(
      icon = {
        Icon(
          Icons.Outlined.Analytics,
          contentDescription = null,
        )
      },
      title = {
        Text(stringResource(Res.string.card_library_totals_title))
      },
    )

    Text(
      text = createOverviewText(
        totalItem = totals.totalItems,
        totalAuthors = totals.totalAuthors,
        totalTime = totals.totalDuration,
        totalSizeInBytes = totals.totalSizeInBytes,
        totalTracks = totals.numAudioTracks,
      ),
      style = MaterialTheme.typography.bodyLarge,
      modifier = Modifier.padding(
        start = 16.dp,
        end = 16.dp,
        bottom = 24.dp,
      ),
    )
  }
}

@Composable
private fun createOverviewText(
  totalItem: Int,
  totalAuthors: Int,
  totalTime: Duration,
  totalTracks: Int,
  totalSizeInBytes: Long,
): AnnotatedString {
  val tokens = stringResource(Res.string.library_totals_summary_format).tokenize()
  return buildAnnotatedString {
    tokens.forEach { token ->
      when (token) {
        is StringToken.Literal -> append(token.value)
        is StringToken.Variable -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
          when (token.key) {
            "items" -> append("$totalItem items")
            "authors" -> append("$totalAuthors authors")
            "time" -> append(totalTime.readoutAtMostHours(ReadoutStyle.Long))
            "tracks" -> append("$totalTracks tracks")
            "size" -> append(totalSizeInBytes.asReadableBytes())
            else -> Unit
          }
        }
      }
    }
  }
}

fun String.tokenize(token: Regex = defaultToken): List<StringToken> {
  return buildList {
    var currentIndex = 0
    token.findAll(this@tokenize).forEach { match ->
      val previousText = substring(currentIndex, match.range.first)
      add(StringToken.Literal(previousText))
      add(StringToken.Variable(match.groupValues[1]))
      currentIndex = match.range.last + 1
    }

    if (currentIndex < length - 1) {
      add(StringToken.Literal(substring(currentIndex)))
    }
  }
}

sealed interface StringToken {
  data class Literal(val value: String) : StringToken
  data class Variable(val key: String) : StringToken
}

private val defaultToken = "\\{\\{(\\w+)\\}\\}".toRegex()
