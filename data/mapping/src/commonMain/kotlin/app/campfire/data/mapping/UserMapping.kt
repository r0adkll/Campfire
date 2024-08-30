package app.campfire.data.mapping

import app.campfire.core.model.User
import app.campfire.data.User as DatabaseUser
import app.campfire.network.models.User as NetworkUser

fun NetworkUser.asDatabaseModel(
  serverUrl: String,
  defaultLibraryId: String,
): DatabaseUser {
  return DatabaseUser(
    id = id,
    name = username,
    type = User.Type.from(type),
    seriesHideFromContinueListening = seriesHideFromContinueListening,
    isActive = isActive,
    isLocked = isLocked,
    lastSeen = lastSeen,
    createdAt = createdAt,
    permission_download = permissions.download,
    permission_update = permissions.update,
    permission_delete = permissions.delete,
    permission_upload = permissions.upload,
    permission_accessAllLibraries = permissions.accessAllLibraries,
    permission_accessAllTags = permissions.accessAllTags,
    permission_accessExplicitContent = permissions.accessExplicitContent,
    librariesAccessible = librariesAccessible,
    itemTagsAccessible = itemTagsAccessible ?: emptyList(),
    selectedLibraryId = defaultLibraryId,
    serverUrl = serverUrl,
  )
}
