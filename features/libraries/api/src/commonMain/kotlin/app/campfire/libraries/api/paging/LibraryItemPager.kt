package app.campfire.libraries.api.paging

import androidx.paging.Pager
import app.campfire.core.model.LibraryItem
import kotlinx.coroutines.flow.Flow

class LibraryItemPager(
  val pager: Pager<Int, LibraryItem>,
  val countFlow: Flow<Int?>,
)
