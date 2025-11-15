package app.campfire.data.mapping

import app.campfire.core.model.FilterData
import app.campfire.network.models.FilterData as NetworkFilterData

fun NetworkFilterData.asDomainModel(): FilterData {
  return FilterData(
    authors = authors.map { it.asDomainModel() },
    genres = genres,
    tags = tags,
    series = series.map { it.asDomainModel() },
    narrators = narrators,
    languages = languages,
    publishers = publishers,
    publishedDecades = publishedDecades,
    bookCount = bookCount,
    authorCount = authorCount,
    seriesCount = seriesCount,
    podcastCount = podcastCount,
  )
}

private fun NetworkFilterData.EntityInfo.asDomainModel(): FilterData.Entity {
  return FilterData.Entity(id, name)
}
