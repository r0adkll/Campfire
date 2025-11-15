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

fun NetworkUser.asDomainModel(
  serverUrl: String,
  defaultLibraryId: String,
): User {
  return User(
    id = id,
    name = username,
    type = User.Type.from(type),
    isActive = isActive,
    isLocked = isLocked,
    lastSeen = lastSeen ?: -1L,
    createdAt = createdAt,
    permissions = User.Permissions(
      download = permissions.download,
      update = permissions.update,
      delete = permissions.delete,
      upload = permissions.upload,
      accessAllLibraries = permissions.accessAllLibraries,
      accessAllTags = permissions.accessAllTags,
      accessExplicitContent = permissions.accessExplicitContent,
    ),
    selectedLibraryId = defaultLibraryId,
    serverUrl = serverUrl,
  )
}

fun DatabaseUser.asDomainModel(): User {
  return User(
    id = id,
    name = name,
    selectedLibraryId = selectedLibraryId,
    type = type,
    isActive = isActive == true,
    isLocked = isLocked == true,
    lastSeen = lastSeen ?: -1L,
    createdAt = createdAt,
    serverUrl = serverUrl,
    permissions = User.Permissions(
      download = permission_download == true,
      update = permission_update == true,
      delete = permission_delete == true,
      upload = permission_upload == true,
      accessAllLibraries = permission_accessAllLibraries == true,
      accessAllTags = permission_accessAllTags == true,
      accessExplicitContent = permission_accessExplicitContent == true,
    ),
  )
}
