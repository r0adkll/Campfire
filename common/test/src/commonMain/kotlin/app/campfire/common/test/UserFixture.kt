package app.campfire.common.test

import app.campfire.core.model.LibraryId
import app.campfire.core.model.User
import app.campfire.core.model.User.Permissions
import app.campfire.core.model.User.Type
import app.campfire.core.model.UserId

fun user(
  id: UserId,
  name: String = "Test User",
  selectedLibraryId: LibraryId = "test_library_id",
  type: Type = Type.Admin,
  isActive: Boolean = true,
  isLocked: Boolean = false,
  lastSeen: Long = 0L,
  createdAt: Long = 0L,
  permissions: Permissions = Permissions(
    download = true,
    update = true,
    delete = true,
    upload = true,
    accessAllLibraries = true,
    accessAllTags = true,
    accessExplicitContent = true,
  ),
  serverUrl: String = "https://test.url",
) = User(
  id = id,
  name = name,
  selectedLibraryId = selectedLibraryId,
  type = type,
  isActive = isActive,
  isLocked = isLocked,
  lastSeen = lastSeen,
  createdAt = createdAt,
  permissions = permissions,
  serverUrl = serverUrl,
)
