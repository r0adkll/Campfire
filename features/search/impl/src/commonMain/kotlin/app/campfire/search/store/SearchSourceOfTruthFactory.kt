package app.campfire.search.store

import app.campfire.CampfireDatabase
import app.campfire.account.api.TokenHydrator
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.data.Search
import app.campfire.data.Search_authors
import app.campfire.data.Search_books
import app.campfire.data.Search_genres
import app.campfire.data.Search_narrators
import app.campfire.data.Search_series
import app.campfire.data.Search_tags
import app.campfire.data.SeriesBookJoin
import app.campfire.data.mapping.asDbModel
import app.campfire.data.mapping.asDomainModel
import app.campfire.data.mapping.dao.LibraryItemDao
import app.campfire.data.mapping.model.mapToLibraryItem
import app.campfire.network.models.SearchResult as NetworkSearchResult
import app.campfire.search.api.SearchResult
import app.campfire.search.mapping.asDomainModel
import app.campfire.search.store.SearchStore.Operation.Query
import app.cash.sqldelight.SuspendingTransactionWithoutReturn
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlin.time.measureTimedValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext
import org.mobilenativefoundation.store.store5.SourceOfTruth

class SearchSourceOfTruthFactory(
  private val db: CampfireDatabase,
  private val libraryItemDao: LibraryItemDao,
  private val tokenHydrator: TokenHydrator,
  private val dispatcherProvider: DispatcherProvider,
) {

  fun create(): SourceOfTruth<Query, NetworkSearchResult, SearchResult> {
    return SourceOfTruth.of(
      reader = ::handleRead,
      writer = { query, result -> handleWrite(query, result) },
      delete = { query -> handleDelete(query) },
    )
  }

  private fun handleRead(query: Query): Flow<SearchResult> {
    return db.searchQueries.select(query.databaseKey)
      .asFlow()
      .mapToOneOrNull(dispatcherProvider.databaseRead)
      .mapNotNull { searchResult ->
        searchResult?.key?.let {
          val (result, duration) = measureTimedValue {
            hydrateSearchResult(it)
          }
          SearchStore.vbark { "Search Result for ${query.text} took $duration" }
          result
        }
      }
  }

  private suspend fun hydrateSearchResult(
    searchKey: String,
  ): SearchResult = withContext(dispatcherProvider.databaseRead) {
    val books = db.searchQueries.searchBooks(searchKey, ::mapToLibraryItem)
      .awaitAsList()
      .map { libraryItemDao.hydrateItem(it) }

    val narrators = db.searchQueries.searchNarrators(searchKey).awaitAsOneOrNull()
      ?: emptyList()

    val authors = db.searchQueries.searchAuthors(searchKey)
      .awaitAsList()
      .map { it.asDomainModel() }

    val series = db.searchQueries.searchSeries(searchKey)
      .awaitAsList()
      .map { series ->
        val seriesBooks = db.libraryItemsQueries.selectForSeries(series.id, ::mapToLibraryItem)
          .awaitAsList()
          .map { libraryItemDao.hydrateItem(it) }

        series.asDomainModel(seriesBooks)
      }

    val tags = db.searchQueries.searchTags(searchKey).awaitAsOneOrNull()
      ?: emptyList()

    val genres = db.searchQueries.searchGenres(searchKey).awaitAsOneOrNull()
      ?: emptyList()

    SearchResult.Success(
      books = books,
      narrators = narrators,
      authors = authors,
      series = series,
      tags = tags,
      genres = genres,
    )
  }

  private suspend fun handleWrite(
    query: Query,
    result: NetworkSearchResult,
  ) = withContext(dispatcherProvider.databaseWrite) {
    SearchStore.vbark { "Writing Search Result: ${result.toShortString()}" }

    db.transaction {
      // Setup transaction notifications
      afterCommit { onWriteSuccess(query, result) }
      afterRollback { onWriteFailed(query, result) }

      // 1) Insert search key
      db.searchQueries.insert(
        Search(
          key = query.databaseKey,
          userId = query.userId,
        ),
      )

      // 2) Insert search result content
      insertSearchResultContents(query, result)

      // 3) Insert search junction keys
      insertSearchJunctions(query, result)
    }
  }

  /**
   * Insert the contents of a search [result] into the database in a single transaction
   * for a provided [query]
   *
   * @param query the search query used to produce the [result]s
   * @param result the search result to store locally
   */
  private suspend fun SuspendingTransactionWithoutReturn.insertSearchResultContents(
    query: Query,
    result: NetworkSearchResult,
  ) {
    val searchKey = query.databaseKey

    result.apply {
      // Insert Books + Relations
      transactionWithResult {
        book.forEach { bookResult ->
          libraryItemDao.insert(bookResult.libraryItem, asTransaction = false)
        }
      }

      // Insert narrators
      db.searchQueries.insertNarrators(
        Search_narrators(
          searchKey = searchKey,
          narrators = narrators.map { it.asDomainModel() },
        ),
      )

      // Insert Authors
      transactionWithResult {
        db.authorsQueries.transaction {
          authors.forEach { author ->
            db.authorsQueries.insert(author.asDbModel(tokenHydrator))
          }
        }
      }

      // Insert Tags
      db.searchQueries.insertTags(
        Search_tags(
          searchKey = searchKey,
          tags = tags.map { it.asDomainModel() },
        ),
      )

      // Insert Genres
      db.searchQueries.insertGenres(
        Search_genres(
          searchKey = searchKey,
          genres = genres.map { it.asDomainModel() },
        ),
      )

      // Insert Series + Books
      transactionWithResult {
        series.forEach { seriesSearchResult ->

          db.seriesQueries.insertOrIgnore(
            seriesSearchResult.series.asDbModel(query.libraryId),
          )

          seriesSearchResult.books.forEach { libraryItem ->
            libraryItemDao.insert(
              item = libraryItem,
              asTransaction = true,
              ignoreOnInsert = true,
            )
            db.seriesBookJoinQueries.insert(
              SeriesBookJoin(
                seriesId = seriesSearchResult.series.id,
                libraryItemId = libraryItem.id,
              ),
            )
          }
        }
      }
    }
  }

  private suspend fun SuspendingTransactionWithoutReturn.insertSearchJunctions(
    query: Query,
    result: NetworkSearchResult,
  ) {
    val searchKey = query.databaseKey

    // Insert Books
    transactionWithResult {
      result.book.forEach { book ->
        db.searchQueries.insertBooks(
          Search_books(
            searchKey = searchKey,
            libraryItemId = book.libraryItem.id,
          ),
        )
      }
    }

    // Insert Authors
    transactionWithResult {
      result.authors.forEach { author ->
        db.searchQueries.insertAuthors(
          Search_authors(
            searchKey = searchKey,
            authorId = author.id,
          ),
        )
      }
    }

    // Insert Series
    transactionWithResult {
      result.series.forEach { series ->
        db.searchQueries.insertSeries(
          Search_series(
            searchKey = searchKey,
            seriesId = series.series.id,
          ),
        )
      }
    }
  }

  private fun onWriteSuccess(query: Query, result: NetworkSearchResult) {
    SearchStore.ibark {
      "Search(${query.text}) successfully inserted: ${result.toShortString()}"
    }
  }

  private fun onWriteFailed(query: Query, result: NetworkSearchResult) {
    SearchStore.ebark {
      "Search(${query.text}) insert failed: ${result.toShortString()}"
    }
  }

  private suspend fun handleDelete(query: Query) = withContext(dispatcherProvider.databaseWrite) {
    db.searchQueries.delete(query.databaseKey)
  }
}
