package app.campfire.core.model

typealias LibraryId = String

data class Library(
  val id: LibraryId,
  val name: String,
  val displayOrder: Int,
  val icon: Icon,
  val mediaType: String,
  val provider: String,
  val coverAspectRatio: Int,
  val audiobooksOnly: Boolean,
  val createdAt: Long,
  val lastUpdate: Long,
) {
  enum class Icon(val networkKey: String) {
    Database("database"),
    AudioBookShelf("audiobookshelf"),
    Books1("books-1"),
    Books2("books-2"),
    Book1("book-1"),
    Microphone1("microphone-1"),
    Microphone3("microphone-3"),
    Radio("radio"),
    Podcast("podcast"),
    Rss("rss"),
    Headphones("headphones"),
    Music("music"),
    FilePicture("file-picture"),
    Rocket("rocket"),
    Power("power"),
    Star("star"),
    Heart("heart"),
    None(""),
    ;

    companion object {
      fun from(networkKey: String) = entries.find { it.networkKey == networkKey } ?: None
    }
  }
}
