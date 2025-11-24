package app.campfire.ui.theming.db

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import app.cash.sqldelight.ColumnAdapter

object ColorListColumnAdapter : ColumnAdapter<List<Color>, String> {
  private const val SEPARATOR = "|"

  override fun decode(databaseValue: String): List<Color> {
    return databaseValue.split(SEPARATOR)
      .mapNotNull { it.toLongOrNull() }
      .map { Color(it) }
  }

  override fun encode(value: List<Color>): String {
    return value.joinToString(SEPARATOR) { it.toArgb().toString() }
  }
}
