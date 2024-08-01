package app.campfire.core.model

typealias UserId = String

data class User(
  val id: UserId,
  val name: String,
  val type: Type,
  val isActive: Boolean,
  val isLocked: Boolean,
  val lastSeen: Long,
  val createdAt: Long,
  val permissions: Permissions,
) {

  enum class Type {
    Root,
    Guest,
    User,
    Admin,
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
