package app.campfire.common.compose.widgets

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.rounded.rememberAnimatedEmptyState
import campfire.common.compose.generated.resources.Res
import campfire.common.compose.generated.resources.empty_screen_quotes
import org.jetbrains.compose.resources.stringArrayResource

private val EmptyImageSize = 278.dp
private val EmptyPaddingHorizontal = 40.dp
private val EmptyVerticalSpacing = 32.dp

@Composable
fun EmptyState(
  message: String,
  modifier: Modifier = Modifier,
) {
  EmptyState(
    message = { Text(message) },
    modifier = modifier,
  )
}

@Composable
fun EmptyState(
  message: @Composable () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = EmptyPaddingHorizontal),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Box(
      modifier = Modifier.weight(.60f),
      contentAlignment = Alignment.BottomCenter,
    ) {
      val emptyPainter = rememberAnimatedEmptyState()
      Image(
        emptyPainter,
        contentDescription = null,
        modifier = Modifier
          .width(EmptyImageSize),
      )
    }

    Spacer(Modifier.height(EmptyVerticalSpacing))

    Box(
      modifier = Modifier.weight(.40f),
      contentAlignment = Alignment.TopCenter,
    ) {
      ProvideTextStyle(
        MaterialTheme.typography.titleMedium.copy(
          textAlign = TextAlign.Center,
          fontWeight = FontWeight.Medium,
        ),
      ) {
        message()
      }
    }
  }
}

@Composable
fun randomEmptyMessage(): String {
  val messages = stringArrayResource(Res.array.empty_screen_quotes)
  return remember { messages.random() }
}
