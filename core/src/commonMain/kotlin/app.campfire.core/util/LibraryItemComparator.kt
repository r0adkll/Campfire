package app.campfire.core.util

import app.campfire.core.model.LibraryItem
import app.campfire.core.settings.SortDirection
import app.campfire.core.settings.SortMode

sealed class LibraryItemComparator : Comparator<LibraryItem> {

  override fun compare(a: LibraryItem, b: LibraryItem): Int {
    return a.media.metadata.title?.compareTo(b.media.metadata.title ?: "") ?: 0
  }

  companion object {
    operator fun invoke(mode: SortMode, direction: SortDirection): Comparator<LibraryItem> {
      val modeComparator = when (mode) {
        SortMode.Title -> TitleComparator
        SortMode.AuthorFL -> AuthorNameComparator
        SortMode.AuthorLF -> AuthorNameLastFirstComparator
        SortMode.PublishYear -> PublishYearComparator
        SortMode.AddedAt -> AddedAtComparator
        SortMode.Size -> SizeComparator
        SortMode.Duration -> DurationComparator
      }

      return when (direction) {
        SortDirection.Ascending -> modeComparator
        SortDirection.Descending -> DescendingComparator(modeComparator)
      }
    }
  }
}

private data object TitleComparator : LibraryItemComparator()

private data object AuthorNameComparator : LibraryItemComparator() {
  override fun compare(a: LibraryItem, b: LibraryItem): Int {
    val result = a.media.metadata.authorName?.compareTo(b.media.metadata.authorName ?: "") ?: 0
    if (result == 0) return super.compare(a, b)
    return result
  }
}

private data object AuthorNameLastFirstComparator : LibraryItemComparator() {
  override fun compare(a: LibraryItem, b: LibraryItem): Int {
    val result = a.media.metadata.authorNameLastFirst?.compareTo(b.media.metadata.authorNameLastFirst ?: "") ?: 0
    if (result == 0) return super.compare(a, b)
    return result
  }
}

private data object PublishYearComparator : LibraryItemComparator() {
  override fun compare(a: LibraryItem, b: LibraryItem): Int {
    val result = a.media.metadata.publishedYear?.compareTo(b.media.metadata.publishedYear ?: "") ?: 0
    if (result == 0) return super.compare(a, b)
    return result
  }
}

private data object AddedAtComparator : LibraryItemComparator() {
  override fun compare(a: LibraryItem, b: LibraryItem): Int {
    val result = a.addedAtMillis.compareTo(b.addedAtMillis)
    if (result == 0) return super.compare(a, b)
    return result
  }
}

private data object SizeComparator : LibraryItemComparator() {
  override fun compare(a: LibraryItem, b: LibraryItem): Int {
    val result = a.sizeInBytes.compareTo(b.sizeInBytes)
    if (result == 0) return super.compare(a, b)
    return result
  }
}

private data object DurationComparator : LibraryItemComparator() {
  override fun compare(a: LibraryItem, b: LibraryItem): Int {
    val result = a.media.durationInMillis.compareTo(b.media.durationInMillis)
    if (result == 0) return super.compare(a, b)
    return result
  }
}

private class DescendingComparator<T>(
  private val comparator: Comparator<T>,
) : Comparator<T> {

  override fun compare(a: T, b: T): Int {
    return comparator.compare(b, a)
  }
}
