package app.campfire.author.api

import app.campfire.core.model.Author
import app.campfire.core.model.User
import app.campfire.core.settings.ContentSortMode
import app.campfire.core.settings.SortDirection
import kotlinx.coroutines.flow.Flow

interface AuthorRepository {

  fun observeAuthors(): Flow<List<Author>>

  fun createAuthorsPager(
    user: User,
    sortMode: ContentSortMode,
    sortDirection: SortDirection,
  ): AuthorPager

  fun observeFilteredAuthorsCount(
    sortMode: ContentSortMode,
    sortDirection: SortDirection,
  ): Flow<Int?>

  fun observeAuthor(authorId: String): Flow<Author>
}
