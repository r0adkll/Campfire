package app.campfire.core.model

data class FilterData(
  val authors: List<Entity> = emptyList(),
  val genres: List<String> = emptyList(),
  val tags: List<String> = emptyList(),
  val series: List<Entity> = emptyList(),
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

  data class Entity(val id: String, val name: String)
}
