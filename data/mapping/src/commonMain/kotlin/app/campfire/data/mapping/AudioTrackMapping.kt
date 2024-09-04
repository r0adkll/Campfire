package app.campfire.data.mapping

import app.campfire.data.MediaAudioTracks
import app.campfire.network.models.AudioTrack

fun AudioTrack.asDbModel(mediaId: String): MediaAudioTracks {
  return MediaAudioTracks(
    mediaId = mediaId,
    mediaIndex = index,
    startOffset = startOffset.toDouble(),
    duration = duration.toDouble(),
    title = title,
    contentUrl = contentUrl,
    mimeType = mimeType,
    codec = codec,
    metadata_filename = metadata.filename,
    metadata_ext = metadata.ext,
    metadata_path = metadata.path,
    metadata_relPath = metadata.relPath,
    metadata_size = metadata.size,
    metadata_mtimeMs = metadata.mtimeMs,
    metadata_ctimeMs = metadata.ctimeMs,
    metadata_birthtimeMs = metadata.birthtimeMs,
  )
}
