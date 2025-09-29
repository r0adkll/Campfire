package app.campfire.network.models

import kotlinx.serialization.Serializable

@Serializable
data class FilterData(
  val authors: List<EntityInfo> = emptyList(),
  val genres: List<String> = emptyList(),
  val tags: List<String> = emptyList(),
  val series: List<EntityInfo> = emptyList(),
  val narrators: List<String> = emptyList(),
  val languages: List<String> = emptyList(),
  val publishers: List<String> = emptyList(),
  val publishedDecades: List<String> = emptyList(),
  val bookCount: Int = 0,
  val authorCount: Int = 0,
  val seriesCount: Int = 0,
  val podcastCount: Int = 0,
  val numIssues: Int = 0,
) {

  @Serializable
  data class EntityInfo(
    val id: String,
    val name: String,
  )
}
