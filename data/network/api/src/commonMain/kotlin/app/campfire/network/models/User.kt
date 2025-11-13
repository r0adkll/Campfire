package app.campfire.network.models

import kotlinx.serialization.Serializable

@Serializable
data class User(
  val id: String,
  val username: String,
  val type: String,
  val token: String,
  val mediaProgress: List<MediaProgress>,
  val seriesHideFromContinueListening: List<String>,
  val bookmarks: List<AudioBookmark>,
  val isActive: Boolean,
  val isLocked: Boolean,
  val lastSeen: Long?,
  val createdAt: Long,
  val permissions: UserPermissions,
  val librariesAccessible: List<String>,
  val itemTagsAccessible: List<String>? = null,
)

@Serializable
data class UserPermissions(
  val download: Boolean,
  val update: Boolean,
  val delete: Boolean,
  val upload: Boolean,
  val accessAllLibraries: Boolean,
  val accessAllTags: Boolean,
  val accessExplicitContent: Boolean,
)
