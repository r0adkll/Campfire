---
name: Podcast Model & API
description: Full podcast model shapes, episode structure, endpoints, playback, and progress for ABS podcast support
type: reference
---

## Library mediaType values
Only two valid values: `"book"` and `"podcast"`. Enforced by `Library.isPodcast` / `Library.isBook` getters and separate DB hooks for each. No other types exist.

## Podcast LibraryItem (minified, from /api/libraries/:id/items)
`media` object has: `id`, `metadata{}`, `coverPath`, `tags[]`, `numEpisodes`, `autoDownloadEpisodes`, `autoDownloadSchedule`, `lastEpisodeCheck` (epoch ms), `maxEpisodesToKeep`, `maxNewEpisodesToDownload`, `size`
- NOT episodes array (minified omits episodes)
- `metadata.type` is the podcast type (e.g. "episodic", "serial") — maps to DB column `podcastType`
- `metadata.titleIgnorePrefix` appears in minified/expanded (not in basic toOldJSON)

## Podcast LibraryItem (expanded, from /api/items/:id?expanded=1)
Same as minified but `media` gains: `libraryItemId`, and `episodes[]` array with full episode objects
- `media.episodes` is only present in `toOldJSONExpanded` (not minified or basic)
- Include `?include=downloads` to get `episodeDownloadsQueued[]` and `episodesDownloading[]` on the item

## PodcastMetadata fields
title, author, description, releaseDate (ISO string), genres[], feedUrl, imageUrl, itunesPageUrl, itunesId, itunesArtistId, explicit (boolean), language, type ("episodic"|"serial")

## PodcastEpisode fields (from toOldJSON)
libraryItemId, podcastId, id, oldEpisodeId (null), index (nullable int), season (string, may be ""), episode (string, e.g. "327"), episodeType ("full"|"trailer"|"bonus"), title, subtitle (string, may be ""), description (HTML), enclosure{url, type, length (string)}, guid (string), pubDate (RFC date string), chapters[], audioFile{...}, publishedAt (epoch ms), addedAt (epoch ms), updatedAt (epoch ms)

## PodcastEpisode fields (from toOldJSONExpanded, e.g. recent-episodes)
All above + audioTrack{...contentUrl, startOffset:0}, size (number), duration (number seconds)

## Episode audioFile shape
index, ino, metadata{filename, ext, path, relPath, size, mtimeMs, ctimeMs, birthtimeMs}, addedAt, updatedAt, trackNumFromMeta (null), discNumFromMeta (null), trackNumFromFilename (null), discNumFromFilename (null), manuallyVerified (bool), exclude (bool), error (null|string), format, duration, bitRate, language (null|string), codec, timeBase, channels, channelLayout, chapters[], embeddedCoverArt (null|string), metaTags{...}, mimeType

## Cover art
Same endpoint for both book and podcast: `GET /api/items/:id/cover` → serves image/webp. coverPath on Podcast model points to local file path.

## MediaProgress for podcasts
- `mediaItemType` = `"podcastEpisode"`, `mediaItemId` = episodeId
- Old serialization: `episodeId` = mediaItemId when podcast episode
- `libraryItemId` stored in `extraData.libraryItemId`
- Progress is per-episode (each episode has its own MediaProgress row)

## PlaybackSession for podcasts
- `episodeId` set to episodeId, `bookId` = null
- `mediaType` = "podcast"
- Single audioTrack per episode (podcast episodes always have exactly 1 track)

## Key source files
- `/workspaces/audiobookshelf/server/models/Podcast.js` — Podcast DB model + serialization
- `/workspaces/audiobookshelf/server/models/PodcastEpisode.js` — Episode DB model + serialization
- `/workspaces/audiobookshelf/server/models/MediaProgress.js` — Polymorphic progress (book|podcastEpisode)
- `/workspaces/audiobookshelf/server/controllers/PodcastController.js` — Podcast-specific endpoints
- `/workspaces/audiobookshelf/server/utils/queries/libraryItemsPodcastFilters.js` — Sort/filter for podcast libraries

## Podcast-specific sort keys (?sort=)
addedAt, size, birthtimeMs, mtimeMs, media.metadata.author, media.metadata.title, media.numTracks (maps to numEpisodes), random

## Podcast-specific filter groups (?filter=)
genres, explicit, recent, feed-open, issues
