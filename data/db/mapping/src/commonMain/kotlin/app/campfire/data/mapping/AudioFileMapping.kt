// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.data.mapping

import app.campfire.core.extensions.seconds
import app.campfire.core.model.AudioFile
import app.campfire.data.MediaAudioFiles
import app.campfire.network.models.AudioFile as NetworkAudioFile
import kotlin.time.DurationUnit

fun NetworkAudioFile.asDbModel(mediaId: String): MediaAudioFiles {
  return MediaAudioFiles(
    mediaId = mediaId,
    mediaIndex = index,
    ino = ino,
    addedAt = addedAt,
    updatedAt = updatedAt,
    trackNumFromMeta = trackNumFromMeta,
    discNumFromMeta = discNumFromMeta,
    trackNumFromFilename = trackNumFromFilename,
    discNumFromFilename = discNumFromFilename,
    manuallyVerified = manuallyVerified,
    invalid = invalid,
    exclude = exclude,
    error = error,
    format = format,
    duration = (duration ?: 0f).toDouble(),
    bitRate = bitRate ?: 0,
    language = language,
    codec = codec.orEmpty(),
    timeBase = timeBase.orEmpty(),
    channels = channels ?: 0,
    channelLayout = channelLayout.orEmpty(),
    embeddedCoverArt = embeddedCoverArt,
    mimeType = mimeType,
  )
}
fun AudioFile.asDbModel(mediaId: String): MediaAudioFiles {
  return MediaAudioFiles(
    mediaId = mediaId,
    mediaIndex = index,
    ino = ino,
    addedAt = addedAt,
    updatedAt = updatedAt,
    trackNumFromMeta = trackNumFromMeta,
    discNumFromMeta = discNumFromMeta,
    trackNumFromFilename = trackNumFromFilename,
    discNumFromFilename = discNumFromFilename,
    manuallyVerified = manuallyVerified,
    invalid = invalid,
    exclude = exclude,
    error = error,
    format = format,
    duration = duration.toDouble(DurationUnit.SECONDS),
    bitRate = bitRate,
    language = language,
    codec = codec,
    timeBase = timeBase,
    channels = channels,
    channelLayout = channelLayout,
    embeddedCoverArt = embeddedCoverArt,
    mimeType = mimeType,
  )
}

fun NetworkAudioFile.asDomainModel(): AudioFile {
  return AudioFile(
    index = index,
    ino = ino,
    addedAt = addedAt,
    updatedAt = updatedAt,
    trackNumFromMeta = trackNumFromMeta,
    discNumFromMeta = discNumFromMeta,
    trackNumFromFilename = trackNumFromFilename,
    discNumFromFilename = discNumFromFilename,
    manuallyVerified = manuallyVerified,
    invalid = invalid,
    exclude = exclude,
    error = error,
    format = format,
    duration = (duration ?: 0f).seconds,
    bitRate = bitRate ?: 0,
    language = language,
    codec = codec.orEmpty(),
    timeBase = timeBase.orEmpty(),
    channels = channels ?: 0,
    channelLayout = channelLayout.orEmpty(),
    embeddedCoverArt = embeddedCoverArt,
    mimeType = mimeType,
  )
}
