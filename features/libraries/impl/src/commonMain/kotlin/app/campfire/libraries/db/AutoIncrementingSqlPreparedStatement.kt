package app.campfire.libraries.db

import app.campfire.core.logging.Cork
import app.cash.sqldelight.db.SqlPreparedStatement

internal class AutoIncrementingSqlPreparedStatement(
  val delegate: SqlPreparedStatement,
  val getAndIncrement: () -> Int,
) : SqlPreparedStatement by delegate, Cork {

  override val tag = "AutoIncrementingSqlPreparedStatement"
  override val enabled: Boolean = false

  private val loggingGetAndIncrement = { method: String ->
    getAndIncrement().also {
      dbark { "~~ getAndIncrement($method) => $it" }
    }
  }

  fun bindBytes(bytes: ByteArray?) {
    bindBytes(loggingGetAndIncrement("bindBytes"), bytes)
  }

  fun bindLong(long: Long?) {
    bindLong(loggingGetAndIncrement("bindLong($long)"), long)
  }

  fun bindDouble(double: Double?) {
    bindDouble(loggingGetAndIncrement("bindDouble($double)"), double)
  }

  fun bindString(string: String?) {
    bindString(loggingGetAndIncrement("bindString($string)"), string)
  }

  fun bindBoolean(boolean: Boolean?) {
    bindBoolean(loggingGetAndIncrement("bindBoolean($boolean)"), boolean)
  }
}
