package app.campfire.auth.models

/*



 */
class AudioBookUser(
  val id: String,
  val username: String,
  val type: String,
  val token: String,
  val isActive: Boolean,
  val isLocked: Boolean,
  val lastSeen: Long,
  val createdAt: Long,
)

class Permissions(
  val download: Boolean,
  val update: Boolean,
  val delete: Boolean,
  val upload: Boolean,
  val accessAllLibraries: Boolean,
  val accessAllTags: Boolean,
  val accessExplicitContent: Boolean,
)
