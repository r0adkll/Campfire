package app.campfire.common.compose.widgets

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import app.campfire.common.compose.layout.LocalContentLayout
import app.campfire.common.compose.layout.cardElevation

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ElevatedContentCard(
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  onClick: (() -> Unit)? = null,
  shape: Shape = MaterialTheme.shapes.largeIncreased,
  colors: CardColors = CardDefaults.elevatedCardColors(
    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
  ),
  content: @Composable ColumnScope.() -> Unit,
) {
  val contentLayout = LocalContentLayout.current
  if (onClick != null) {
    ElevatedCard(
      modifier = modifier,
      enabled = enabled,
      onClick = onClick,
      shape = shape,
      colors = colors,
      elevation = contentLayout.cardElevation,
      content = content,
    )
  } else {
    ElevatedCard(
      modifier = modifier,
      shape = shape,
      colors = colors,
      elevation = contentLayout.cardElevation,
      content = content,
    )
  }
}
