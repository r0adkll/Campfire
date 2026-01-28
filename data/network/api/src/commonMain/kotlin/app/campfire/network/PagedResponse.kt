package app.campfire.network

data class PagedResponse<Data>(
  val data: List<Data>,
  val page: Int,
  val limit: Int,
  val total: Int,
  val offset: Int,
)

val PagedResponse<*>.nextPage: Int?
  get() = if (offset + data.size < total) {
    page + 1
  } else {
    null
  }
