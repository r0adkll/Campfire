package app.campfire.data.mapping

import app.campfire.account.api.UrlHydrator
import app.campfire.core.model.AudioTrack
import app.campfire.core.model.FileMetadata
import app.campfire.core.model.MetaTags
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
    metaTags_tagAlbum = metaTags?.tagAlbum,
    metaTags_tagArtist = metaTags?.tagArtist,
    metaTags_tagAlbumArtist = metaTags?.tagAlbumArtist,
    metaTags_tagTitle = metaTags?.tagTitle,
    metaTags_tagSubtitle = metaTags?.tagSubtitle,
    metaTags_tagSeries = metaTags?.tagSeries,
    metaTags_tagSeriesPart = metaTags?.tagSeriesPart,
    metaTags_tagTrack = metaTags?.tagTrack,
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
    metaTags_tagAlbum = metaTags?.tagAlbum,
    metaTags_tagArtist = metaTags?.tagArtist,
    metaTags_tagAlbumArtist = metaTags?.tagAlbumArtist,
    metaTags_tagTitle = metaTags?.tagTitle,
    metaTags_tagSubtitle = metaTags?.tagSubtitle,
    metaTags_tagSeries = metaTags?.tagSeries,
    metaTags_tagSeriesPart = metaTags?.tagSeriesPart,
    metaTags_tagTrack = metaTags?.tagTrack,
  )
}

fun NetworkAudioTrack.asDomainModel(urlHydrator: UrlHydrator): AudioTrack {
  return AudioTrack(
    index = index,
    startOffset = startOffset,
    duration = duration,
    title = title,
    contentUrl = contentUrl,
    mimeType = mimeType,
    codec = codec,
    metadata = FileMetadata(
      filename = metadata.filename,
      ext = metadata.ext,
      path = urlHydrator.hydrateUrl(metadata.path),
      relPath = metadata.relPath,
      size = metadata.size,
      mtimeMs = metadata.mtimeMs,
      ctimeMs = metadata.ctimeMs,
      birthtimeMs = metadata.birthtimeMs,
    ),
    metaTags = metaTags?.let {
      MetaTags(
        tagAlbum = it.tagAlbum,
        tagArtist = it.tagArtist,
        tagAlbumArtist = it.tagAlbumArtist,
        tagTitle = it.tagTitle,
        tagSubtitle = it.tagSubtitle,
        tagSeries = it.tagSeries,
        tagSeriesPart = it.tagSeriesPart,
        tagTrack = it.tagTrack,
      )
    },
  )
}
