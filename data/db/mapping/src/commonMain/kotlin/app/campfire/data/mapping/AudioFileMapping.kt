// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.data.mapping

import app.campfire.core.extensions.seconds
import app.campfire.core.model.AudioFile
import app.campfire.data.MediaAudioFiles
import app.campfire.network.models.AudioFile as NetworkAudioFile
import kotlin.time.DurationUnit

/**
 * [fallbackIndex] stands in for a missing [NetworkAudioFile.index]: the file's 1-based position
 * in its item's list, which is how the server numbers them.
 */
fun NetworkAudioFile.asDbModel(mediaId: String, fallbackIndex: Int): MediaAudioFiles {
  return MediaAudioFiles(
    mediaId = mediaId,
    mediaIndex = index ?: fallbackIndex,
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
    duration = duration.toDouble(),
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

/**
 * [fallbackIndex] stands in for a missing [NetworkAudioFile.index], as in [asDbModel].
 */
fun NetworkAudioFile.asDomainModel(fallbackIndex: Int): AudioFile {
  return AudioFile(
    index = index ?: fallbackIndex,
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
    duration = duration.seconds,
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
