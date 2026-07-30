// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.data.mapping

import app.campfire.account.api.UrlHydrator
import app.campfire.core.model.AudioTrack
import app.campfire.core.model.FileMetadata
import app.campfire.core.model.MetaTags
import app.campfire.core.model.PodcastEpisodeId
import app.campfire.data.MediaAudioTracks
import app.campfire.data.PodcastEpisodeAudioTrack as DbPodcastEpisodeAudioTrack
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

fun NetworkAudioTrack.asEpisodeDbModel(episodeId: PodcastEpisodeId): DbPodcastEpisodeAudioTrack {
  return DbPodcastEpisodeAudioTrack(
    episodeId = episodeId,
    trackIndex = index,
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

fun AudioTrack.asEpisodeDbModel(episodeId: PodcastEpisodeId): DbPodcastEpisodeAudioTrack {
  return DbPodcastEpisodeAudioTrack(
    episodeId = episodeId,
    trackIndex = index,
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

fun DbPodcastEpisodeAudioTrack.asDomainModel(urlHydrator: UrlHydrator): AudioTrack {
  return AudioTrack(
    index = trackIndex,
    startOffset = startOffset.toFloat(),
    duration = duration.toFloat(),
    title = title,
    contentUrl = urlHydrator.hydrateUrl(contentUrl),
    mimeType = mimeType,
    codec = codec,
    metadata = FileMetadata(
      filename = metadata_filename,
      ext = metadata_ext,
      path = urlHydrator.hydrateUrl(metadata_path),
      relPath = metadata_relPath,
      size = metadata_size,
      mtimeMs = metadata_mtimeMs,
      ctimeMs = metadata_ctimeMs,
      birthtimeMs = metadata_birthtimeMs,
    ),
    metaTags = if (
      metaTags_tagAlbum != null ||
      metaTags_tagArtist != null ||
      metaTags_tagAlbumArtist != null ||
      metaTags_tagTitle != null ||
      metaTags_tagSubtitle != null ||
      metaTags_tagSeries != null ||
      metaTags_tagSeriesPart != null ||
      metaTags_tagTrack != null
    ) {
      MetaTags(
        tagAlbum = metaTags_tagAlbum,
        tagArtist = metaTags_tagArtist,
        tagAlbumArtist = metaTags_tagAlbumArtist,
        tagTitle = metaTags_tagTitle,
        tagSubtitle = metaTags_tagSubtitle,
        tagSeries = metaTags_tagSeries,
        tagSeriesPart = metaTags_tagSeriesPart,
        tagTrack = metaTags_tagTrack,
      )
    } else {
      null
    },
  )
}

fun NetworkAudioTrack.asDomainModel(urlHydrator: UrlHydrator): AudioTrack {
  return AudioTrack(
    index = index,
    startOffset = startOffset,
    duration = duration,
    title = title,
    contentUrl = urlHydrator.hydrateUrl(contentUrl),
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
