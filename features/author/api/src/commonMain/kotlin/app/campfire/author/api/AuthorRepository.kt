package app.campfire.author.api

import app.campfire.core.model.Author
import app.campfire.core.settings.ContentSortMode
import app.campfire.core.settings.SortDirection
import kotlinx.coroutines.flow.Flow

interface AuthorRepository {

  fun observeAuthors(): Flow<List<Author>>

  fun observeAuthorsPager(
    sortMode: ContentSortMode,
    sortDirection: SortDirection,
  ): Flow<AuthorPager>

  fun observeAuthor(authorId: String): Flow<Author>
}
