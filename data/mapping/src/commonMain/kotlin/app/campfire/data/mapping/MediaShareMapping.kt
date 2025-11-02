package app.campfire.data.mapping

import app.campfire.core.model.MediaShare
import app.campfire.core.model.UserId
import app.campfire.data.MediaShare as DbMediaShare
import app.campfire.network.envelopes.MediaShareResponse

fun DbMediaShare.asDomainModel(): MediaShare {
  return MediaShare(
    id = id,
    slug = slug,
    url = url,
    libraryItemId = libraryItemId,
    isDownloadable = isDownloadable,
    expiresAt = expiresAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
  )
}

fun MediaShareResponse.asDbModel(userId: UserId): DbMediaShare {
  return DbMediaShare(
    id = id,
    slug = slug,
    url = shareableUrl,
    libraryItemId = mediaItemId,
    isDownloadable = isDownloadable,
    expiresAt = expiresAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    userId = userId,
  )
}

fun MediaShareResponse.asDomainModel(): MediaShare {
  return MediaShare(
    id = id,
    slug = slug,
    url = shareableUrl,
    libraryItemId = mediaItemId,
    isDownloadable = isDownloadable,
    expiresAt = expiresAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
  )
}
