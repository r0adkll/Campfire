package app.campfire.data.mapping

import app.campfire.account.api.TokenHydrator
import app.campfire.core.model.DeviceInfo
import app.campfire.core.model.PlaybackSession
import app.campfire.network.models.DeviceInfo as NetworkDeviceInfo
import app.campfire.network.models.PlaybackSession as NetworkPlaybackSession
import kotlin.time.Duration.Companion.seconds
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

suspend fun NetworkPlaybackSession.asDomainModel(tokenHydrator: TokenHydrator): PlaybackSession {
  return PlaybackSession(
    id = id,
    userId = userId,
    libraryId = libraryId,
    libraryItemId = libraryItemId,
    episodeId = episodeId,
    mediaType = mediaType,
    mediaMetadata = mediaMetadata.asDomainModel(),
    chapters = chapters.map { it.asDomainModel() },
    displayTitle = displayTitle,
    displayAuthor = displayAuthor,
    coverImageUrl = tokenHydrator.hydrateLibraryItem(libraryItemId),
    duration = duration.seconds,
    playMethod = playMethod,
    mediaPlayer = mediaPlayer,
    deviceInfo = deviceInfo.asDomainModel(),
    serverVersion = serverVersion,
    date = date,
    dayOfWeek = dayOfWeek,
    timeListening = timeListening ?: 0f,
    startTime = startTime,
    currentTime = currentTime,
    startedAt = Instant.fromEpochMilliseconds(startedAt).toLocalDateTime(TimeZone.currentSystemDefault()),
    updatedAt = Instant.fromEpochMilliseconds(updatedAt).toLocalDateTime(TimeZone.currentSystemDefault()),
  )
}

private fun NetworkDeviceInfo.asDomainModel(): DeviceInfo {
  return DeviceInfo(
    id = id,
    userId = userId,
    deviceId = deviceId,
    ipAddress = ipAddress,
    browserName = browserName,
    browserVersion = browserVersion,
    osName = osName,
    osVersion = osVersion,
    deviceType = deviceType,
    manufacturer = manufacturer,
    model = model,
    sdkVersion = sdkVersion,
    clientName = clientName,
    clientVersion = clientVersion,
  )
}
