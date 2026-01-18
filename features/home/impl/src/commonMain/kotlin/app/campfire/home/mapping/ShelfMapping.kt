package app.campfire.home.mapping

import app.campfire.core.model.LibraryId
import app.campfire.core.model.ShelfType
import app.campfire.core.model.UserId
import app.campfire.data.Shelf as DbShelf
import app.campfire.home.api.model.Shelf as DomainShelf
import app.campfire.home.store.home.uniqueId
import app.campfire.network.models.Shelf as NetworkShelf

fun DbShelf.asDomainModel(): DomainShelf {
  return DomainShelf(
    id = id,
    label = label,
    total = total,
    type = type,
    order = homeOrder,
  )
}

fun NetworkShelf.asDbModel(
  index: Int,
  libraryId: LibraryId,
  userId: UserId,
): DbShelf {
  return DbShelf(
    id = uniqueId(userId, libraryId),
    libraryId = libraryId,
    userId = userId,
    label = label,
    labelStringKey = labelStringKey,
    total = total,
    homeOrder = index,
    type = when (this) {
      is NetworkShelf.AuthorShelf -> ShelfType.AUTHOR
      is NetworkShelf.BookShelf -> ShelfType.BOOK
      is NetworkShelf.EpisodeShelf -> ShelfType.EPISODE
      is NetworkShelf.PodcastShelf -> ShelfType.PODCAST
      is NetworkShelf.SeriesShelf -> ShelfType.SERIES
    },
  )
}
