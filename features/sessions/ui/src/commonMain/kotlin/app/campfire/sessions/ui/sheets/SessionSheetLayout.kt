package app.campfire.sessions.ui.sheets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun SessionSheetLayout(
  title: @Composable BoxScope.() -> Unit,
  modifier: Modifier = Modifier,
  trailingContent: @Composable BoxScope.() -> Unit = {},
  content: @Composable ColumnScope.() -> Unit,
) {
  Column(
    modifier = modifier,
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .height(56.dp),
      contentAlignment = Alignment.Center,
    ) {
      ProvideTextStyle(
        MaterialTheme.typography.titleLarge.copy(
          fontWeight = FontWeight.SemiBold,
        ),
      ) {
        title()
      }

      trailingContent()
    }

    content()
  }
}
