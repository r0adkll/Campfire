package app.campfire.author.api

import androidx.paging.Pager
import app.campfire.core.model.Author
import kotlinx.coroutines.flow.Flow

class AuthorPager(
  val pager: Pager<Int, Author>,
  val countFlow: Flow<Int?>,
)
