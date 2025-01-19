package app.campfire.common.compose.widgets

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.ParkBench

private val EmptyImageSize = 128.dp
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
    Image(
      Icons.ParkBench,
      contentDescription = null,
      modifier = Modifier
        .size(EmptyImageSize),
    )

    Spacer(Modifier.height(EmptyVerticalSpacing))

    ProvideTextStyle(
      MaterialTheme.typography.titleMedium.copy(
        textAlign = TextAlign.Center,
      ),
    ) {
      message()
    }
  }
}
