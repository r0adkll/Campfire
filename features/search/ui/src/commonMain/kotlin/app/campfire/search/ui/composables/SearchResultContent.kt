package app.campfire.search.ui.composables

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.layout.ContentLayout
import app.campfire.common.compose.layout.LocalContentLayout
import app.campfire.common.compose.widgets.AuthorCard
import app.campfire.common.compose.widgets.EmptyState
import app.campfire.common.compose.widgets.ItemCollectionCard
import app.campfire.common.compose.widgets.LibraryItemCard
import app.campfire.common.compose.widgets.LoadingState
import app.campfire.core.model.Author
import app.campfire.core.model.BasicSearchResult
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.Series
import app.campfire.core.offline.OfflineStatus
import app.campfire.search.api.SearchResult
import campfire.features.search.ui.generated.resources.Res
import campfire.features.search.ui.generated.resources.header_authors
import campfire.features.search.ui.generated.resources.header_books
import campfire.features.search.ui.generated.resources.header_genres
import campfire.features.search.ui.generated.resources.header_narrators
import campfire.features.search.ui.generated.resources.header_series
import campfire.features.search.ui.generated.resources.header_tags
import campfire.features.search.ui.generated.resources.search_results_empty_message
import campfire.features.search.ui.generated.resources.search_results_error_message
import com.slack.circuit.sharedelements.SharedElementTransitionLayout
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import org.jetbrains.compose.resources.stringResource

private val SearchEmptyImageSize = 200.dp

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun SearchResultContent(
  query: String,
  results: SearchResult,
  offlineStatus: (LibraryItemId) -> OfflineStatus,
  onBookClick: (LibraryItem) -> Unit,
  onAuthorClick: (Author) -> Unit,
  onSeriesClick: (Series) -> Unit,
  onTagClick: (BasicSearchResult) -> Unit,
  onGenreClick: (BasicSearchResult) -> Unit,
  onNarratorClick: (BasicSearchResult) -> Unit,
  modifier: Modifier = Modifier,
) = SharedElementTransitionLayout {
  when (results) {
    SearchResult.Error -> EmptyState(
      message = stringResource(resource = Res.string.search_results_error_message),
      modifier = modifier.padding(vertical = 16.dp),
      imageSize = SearchEmptyImageSize,
    )
    SearchResult.Loading -> LoadingState(modifier.fillMaxSize())
    is SearchResult.Success -> if (results.isEmpty && query.isNotBlank()) {
      EmptyState(
        message = {
          Text(
            text = buildAnnotatedString {
              append(stringResource(Res.string.search_results_empty_message))
              append(" ")
              withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append("\"")
                append(query)
                append("\"")
              }
            },
          )
        },
        modifier = modifier.padding(vertical = 16.dp),
        imageSize = SearchEmptyImageSize,
      )
    } else if (results.isEmpty && query.isBlank()) {
      EmptyState(
        message = "Your next adventure is just a \nsearch away!",
        modifier = modifier.padding(vertical = 16.dp),
        imageSize = SearchEmptyImageSize,
      )
    } else {
      CompositionLocalProvider(
        LocalContentLayout provides ContentLayout.Root,
      ) {
        SuccessContent(
          result = results,
          offlineStatus = offlineStatus,
          onBookClick = onBookClick,
          onNarratorClick = onNarratorClick,
          onAuthorClick = onAuthorClick,
          onSeriesClick = onSeriesClick,
          onTagClick = onTagClick,
          onGenreClick = onGenreClick,
          modifier = modifier,
        )
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SuccessContent(
  result: SearchResult.Success,
  offlineStatus: (LibraryItemId) -> OfflineStatus,
  onBookClick: (LibraryItem) -> Unit,
  onAuthorClick: (Author) -> Unit,
  onSeriesClick: (Series) -> Unit,
  onTagClick: (BasicSearchResult) -> Unit,
  onGenreClick: (BasicSearchResult) -> Unit,
  onNarratorClick: (BasicSearchResult) -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyVerticalGrid(
    columns = GridCells.Adaptive(100.dp),
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
    contentPadding = PaddingValues(
      horizontal = 16.dp,
      vertical = 8.dp,
    ),
  ) {
    // Books
    result.books.ifNotEmpty { books ->
      headerItem("books") { stringResource(Res.string.header_books) }
      items(
        items = books,
        key = { it.id },
      ) { book ->
        LibraryItemCard(
          item = book,
          offlineStatus = offlineStatus(book.id),
          onClick = { onBookClick(book) },
          modifier = Modifier.animateItem(),
          colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
          ),
        )
      }
    }

    // Authors
    result.authors.ifNotEmpty { authors ->
      headerItem("authors") { stringResource(Res.string.header_authors) }
      items(
        items = authors,
        key = { it.id },
      ) { author ->
        AuthorCard(
          author = author,
          onClick = { onAuthorClick(author) },
          modifier = Modifier.animateItem(),
          colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
          ),
        )
      }
    }

    // Series
    result.series.ifNotEmpty { series ->
      headerItem("series") { stringResource(Res.string.header_series) }
      items(
        items = series,
        key = { it.id },
        span = { GridItemSpan(2) },
      ) { s ->
        ItemCollectionCard(
          sharedTransitionKey = s.id,
          name = s.name,
          description = s.description,
          items = s.books
            ?.sortedBy { it.media.metadata.seriesSequence?.sequence }
            ?: emptyList(),
          itemSize = 100.dp,
          onClick = { onSeriesClick(s) },
          colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
          ),
          modifier = Modifier
            .fillMaxWidth()
            .animateItem(),
        )
      }
    }

    // Narrators
    result.narrators.ifNotEmpty { narrators ->
      headerItem("narrators") { stringResource(Res.string.header_narrators) }
      item(
        span = { GridItemSpan(maxLineSpan) },
      ) {
        FlowRow(
          modifier = modifier,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          narrators.forEach { narrator ->
            BasicSearchResultChip(
              result = narrator,
              modifier = Modifier.clickable {
                onNarratorClick(narrator)
              },
            )
          }
        }
      }
    }

    // Tags
    result.tags.ifNotEmpty { tags ->
      headerItem("tags") { stringResource(Res.string.header_tags) }
      item(
        span = { GridItemSpan(maxLineSpan) },
      ) {
        FlowRow(
          modifier = modifier.animateItem(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          tags.forEach { tag ->
            BasicSearchResultChip(
              result = tag,
              modifier = Modifier.clickable {
                onTagClick(tag)
              },
            )
          }
        }
      }
    }

    // Narrators
    result.genres.ifNotEmpty { genres ->
      headerItem("genres") { stringResource(Res.string.header_genres) }
      item(
        span = { GridItemSpan(maxLineSpan) },
      ) {
        FlowRow(
          modifier = modifier,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          genres.forEach { genre ->
            BasicSearchResultChip(
              result = genre,
              modifier = Modifier.clickable {
                onGenreClick(genre)
              },
            )
          }
        }
      }
    }

    item { Spacer(Modifier.height(16.dp)) }
  }
}

fun LazyGridScope.headerItem(key: Any?, title: @Composable () -> String) {
  item(
    key = key,
    span = { GridItemSpan(maxLineSpan) },
    contentType = "header",
  ) {
    Text(
      text = title(),
      style = MaterialTheme.typography.labelLarge,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier
        .padding(
          vertical = 16.dp,
        )
        .animateItem(),
    )
  }
}

@OptIn(ExperimentalContracts::class)
private inline fun <T> List<T>.ifNotEmpty(block: (List<T>) -> Unit) {
  contract {
    callsInPlace(block, InvocationKind.AT_MOST_ONCE)
  }
  if (isNotEmpty()) block(this)
}
