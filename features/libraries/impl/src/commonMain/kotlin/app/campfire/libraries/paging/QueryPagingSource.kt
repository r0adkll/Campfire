package app.campfire.libraries.paging

import androidx.paging.PagingSource
import app.campfire.core.logging.Cork
import app.cash.sqldelight.Query
import kotlin.properties.Delegates

abstract class QueryPagingSource<Key : Any, RowType : Any, DatabaseType : Any> :
  PagingSource<Key, RowType>(),
  Query.Listener,
  Cork {

  protected var currentQuery: Query<DatabaseType>? by Delegates.observable(null) { _, old, new ->
    vbark { "Current Query Changed($this), $old" }
    old?.removeListener(this)
    new?.addListener(this)
  }

  init {
    registerInvalidatedCallback {
      currentQuery?.removeListener(this)
      currentQuery = null
    }
  }

  final override fun queryResultsChanged() {
    vbark { "Query Results Changed($this)" }
    invalidate()
  }
}
