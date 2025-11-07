package app.campfire.core.model

typealias UserId = String

data class User(
  val id: UserId,
  val name: String,
  val selectedLibraryId: LibraryId,
  val type: Type,
  val isActive: Boolean,
  val isLocked: Boolean,
  val lastSeen: Long,
  val createdAt: Long,
  val permissions: Permissions,
  val serverUrl: String,
) {

  enum class Type {
    Root,
    Guest,
    User,
    Admin,
    ;

    companion object {
      fun from(value: String): Type {
        return when (value.lowercase()) {
          "root" -> Root
          "guest" -> Guest
          "user" -> User
          "admin" -> Admin
          else -> throw IllegalArgumentException("Unknown user type: $value")
        }
      }
    }
  }

  data class Permissions(
    val download: Boolean,
    val update: Boolean,
    val delete: Boolean,
    val upload: Boolean,
    val accessAllLibraries: Boolean,
    val accessAllTags: Boolean,
    val accessExplicitContent: Boolean,
  )
}
