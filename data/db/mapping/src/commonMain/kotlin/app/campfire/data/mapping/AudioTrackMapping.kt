package app.campfire.data.mapping

import app.campfire.account.api.TokenHydrator
import app.campfire.core.model.AudioTrack
import app.campfire.core.model.FileMetadata
import app.campfire.data.MediaAudioTracks
import app.campfire.network.models.AudioTrack as NetworkAudioTrack

fun NetworkAudioTrack.asDbModel(mediaId: String): MediaAudioTracks {
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

suspend fun NetworkAudioTrack.asDomainModel(tokenHydrator: TokenHydrator): AudioTrack {
  return AudioTrack(
    index = index,
    startOffset = startOffset,
    duration = duration,
    title = title,
    contentUrl = contentUrl,
    contentUrlWithToken = tokenHydrator.hydrateUrlWithToken(contentUrl),
    mimeType = mimeType,
    codec = codec,
    metadata = FileMetadata(
      filename = metadata.filename,
      ext = metadata.ext,
      path = tokenHydrator.hydrateUrl(metadata.path),
      relPath = metadata.relPath,
      size = metadata.size,
      mtimeMs = metadata.mtimeMs,
      ctimeMs = metadata.ctimeMs,
      birthtimeMs = metadata.birthtimeMs,
    ),
  )
}
