package app.campfire.user.test.fixtures

import app.campfire.core.model.User
import app.campfire.core.model.UserId

fun user(
  id: UserId,
): User {
  return User(
    id = id,
    name = "fake_user",
    selectedLibraryId = "fake_library_id",
    type = User.Type.Root,
    isActive = true,
    isLocked = false,
    lastSeen = 0L,
    createdAt = 0L,
    permissions = User.Permissions(
      download = true,
      update = true,
      delete = true,
      upload = true,
      accessAllLibraries = true,
      accessAllTags = true,
      accessExplicitContent = true,
    ),
    serverUrl = "fake_server_url",
  )
}
