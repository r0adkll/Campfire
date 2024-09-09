package app.campfire.author.api

import app.campfire.core.model.Author
import kotlinx.coroutines.flow.Flow

interface AuthorRepository {

  fun observeAuthors(): Flow<List<Author>>

  fun observeAuthor(authorId: String): Flow<Author>
}
