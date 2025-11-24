package app.campfire.ui.theming.db

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import app.cash.sqldelight.ColumnAdapter

object ColorColumnAdapter : ColumnAdapter<Color, Long> {
  override fun decode(databaseValue: Long): Color = Color(databaseValue)
  override fun encode(value: Color): Long = value.toArgb().toLong()
}
