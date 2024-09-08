package app.campfire.data.mapping

import app.campfire.data.MediaAudioFiles
import app.campfire.network.models.AudioFile as NetworkAudioFile

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
