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

## Episode Search & Download Flow

### GET /api/podcasts/:id/search-episode
- **Source**: `PodcastController.findEpisode` → `podcastUtils.findMatchingEpisodes`
- **Auth**: Any authenticated user (no admin check in this method, but middleware checks library access)
- **Query param**: `title` (REQUIRED, string). Using `q`, `query`, `term` all return HTTP 500 (server treats missing/wrong-typed title as error, not 400)
- **Mechanism**: Fetches full RSS feed live from `media.feedURL`, then runs Fuse.js fuzzy search against all episode titles (weight 0.7) and subtitles (weight 0.3). Threshold=0.4 (0=exact, 1=anything).
- **Returns RSS-derived data — not matched against what's already downloaded**. The client must filter out already-downloaded episodes (by comparing guid or enclosure URL) if needed.
- **Response envelope**: `{ episodes: [ { episode: RssPodcastEpisode }, ... ] }` — each result is wrapped in `{ episode: ... }`, NOT a flat array of episodes.
- **No pagination**. Returns all fuzzy matches up to Fuse.js threshold.
- **RssPodcastEpisode shape** (live-confirmed):
  ```
  title: String
  subtitle: String (may be "")
  description: String (HTML-sanitized, may be "")
  descriptionPlain: String (plain text version)
  pubDate: String (RFC 2822 date e.g. "Wed, 13 Aug 2025 16:00:00 +0000")
  episodeType: String ("full"|"trailer"|"bonus", may be "")
  season: String (may be "")
  episode: String (may be "")
  author: String (may be "")
  duration: String (HH:MM:SS or MM:SS format, may be "")
  durationSeconds: Number|null (parsed from duration)
  explicit: String ("true"|"false"|"")
  publishedAt: Number|null (epoch ms)
  enclosure: {
    url: String (audio file URL)
    type: String ("audio/mpeg" etc, may be absent)
    length: String (bytes as string, may be absent)
  }
  guid: String|null
  chaptersUrl: String|null
  chaptersType: String|null
  chapters: Array (usually [])
  ```
- **NOT present on RSS episodes** (vs downloaded PodcastEpisode): id, libraryItemId, podcastId, index, audioFile, audioTrack, size, addedAt, updatedAt

### POST /api/podcasts/:id/download-episodes
- **Source**: `PodcastController.downloadEpisodes` → `PodcastManager.downloadPodcastEpisodes`
- **Auth**: ADMIN or UP required (returns 403 for regular users)
- **Body**: JSON array of RssPodcastEpisode objects (exactly the `episode` field objects from search results, not wrapped).
  - Minimum required fields: `enclosure.url` (used to derive the download URL), `title` (used for filename)
  - Best practice: send the full episode object from search results
  - Example: `POST` body = `[{ "title": "...", "enclosure": { "url": "https://..." }, ... }]`
- **Response**: HTTP 200 (no body). Fire-and-forget: downloads happen asynchronously.
- **Dedup**: Server silently skips episodes already in the queue for the same library item + URL.

### GET /api/podcasts/:id/downloads
- **Source**: `PodcastController.getEpisodeDownloads` → `PodcastManager.getEpisodeDownloadsInQueue`
- **Auth**: Any authenticated user with library access
- **Response**: `{ downloads: [ PodcastEpisodeDownload ] }` — only the queue for this specific libraryItem
- **PodcastEpisodeDownload (toJSONForClient) shape**:
  ```
  id: String (uuid)
  episodeDisplayTitle: String|null
  url: String (encoded download URL)
  libraryItemId: String|null
  libraryId: String|null
  isFinished: Boolean
  failed: Boolean
  appendRandomId: Boolean
  startedAt: Number|null (epoch ms)
  createdAt: Number (epoch ms)
  finishedAt: Number|null (epoch ms)
  podcastTitle: String|null
  podcastExplicit: Boolean
  season: String|null
  episode: String|null
  episodeType: String ("full" default)
  publishedAt: Number|null (epoch ms)
  guid: String|null
  ```

### GET /api/libraries/:id/episode-downloads
- **Source**: `LibraryController.getEpisodeDownloadQueue` → `PodcastManager.getDownloadQueueDetails`
- **Auth**: Any authenticated user
- **Response**: `{ currentDownload: PodcastEpisodeDownload|null, queue: PodcastEpisodeDownload[] }` — library-wide view, not per-podcast
- Use this endpoint to monitor global queue state across all podcasts in a library.

### GET /api/podcasts/:id/checknew
- **Auth**: ADMIN required
- **Query**: `?limit=N` (default 3) — max new episodes to auto-download
- **Response**: `{ episodes: RssPodcastEpisode[] }` — episodes that were newly found and queued for download. Returns `[]` if nothing new or no RSS feed.
- **Side effect**: actually triggers downloads; not just a check.

### GET /api/podcasts/:id/clear-queue
- **Auth**: ADMIN required
- **Response**: HTTP 200 (no body)
- Emits socket event `episode_download_queue_cleared` with libraryItemId.

### Socket.io Download Events
All download events emit `PodcastEpisodeDownload.toJSONForClient()` as payload:
- `episode_download_queued` — episode added to queue (not yet downloading)
- `episode_download_started` — download actively started
- `episode_download_finished` — download completed (check `failed` field for success/failure)
- `episode_download_queue_cleared` — only emits `libraryItemId` string (not full download object)

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

## Latest Episodes Endpoints (for "Latest Episodes" screen)

### Primary: GET /api/libraries/:id/recent-episodes
- **Source**: `LibraryController.getRecentEpisodes` → `libraryItemsPodcastFilters.getRecentEpisodes`
- **Source file**: `/workspaces/audiobookshelf/server/utils/queries/libraryItemsPodcastFilters.js` line 506
- **Requires**: podcast library only (returns 404 for book libraries), Bearer token auth
- **Sort**: `publishedAt DESC` (publish date, not addedAt)
- **Filter**: Hides episodes where `isFinished=true` for the requesting user. Does NOT have the 60-day window.
- **Pagination**: `?limit=N&page=P` (page is 0-indexed, offset = page*limit). limit=0 means 0 results from Sequelize.
- **No total count in response** — envelope: `{ episodes: [...], limit: N, page: P }`
- **Episode shape**: `toOldJSONExpanded` + `podcast` object + `libraryId`
  - Top-level: libraryItemId, podcastId, id, oldEpisodeId, index, season, episode, episodeType, title, subtitle, description, enclosure{url,type,length}, guid, pubDate, chapters, audioFile, publishedAt, addedAt, updatedAt, audioTrack, size, duration, podcast{...}, libraryId
  - `podcast` is a full `toOldJSON()` of the Podcast (has metadata.title, metadata.author, metadata.imageUrl, libraryItemId, coverPath, etc.) with `episodes:[]` (emptied)
  - No MediaProgress inline — must fetch separately via `/api/me/progress/:libraryItemId/:episodeId`

### Secondary: GET /api/libraries/:id/personalized (newest-episodes shelf)
- **Source**: `LibraryController.getUserPersonalizedShelves` → `LibraryItem.getPersonalizedShelves` → `libraryFilters.getNewestPodcastEpisodes`
- Uses `getFilteredPodcastEpisodes(libraryId, user, 'recent', null, 'createdAt', true, limit, 0)` — `filterGroup='recent'` means ONLY episodes added in last 60 days, sorted by `createdAt DESC` (not publishedAt)
- **Entity shape in shelf**: LibraryItem minified (`toOldJSONMinified`) + `recentEpisode` property (episode as `toOldJSON` — not expanded, no audioTrack/size/duration)
- `?limit=N` controls items per shelf (default 10)
- Returns all podcast shelves at once: continue-listening, newest-episodes, recently-added — not just episodes
- Shelf `type: "episode"`, `total: N` gives total count

### Recommendation
Use `/api/libraries/:id/recent-episodes` for the dedicated Latest Episodes screen:
- Returns full expanded episode objects with podcast context in one call
- True pagination support (limit + page)
- Sorted by publishedAt (publish date) across all podcasts in library
- Automatically hides finished episodes (good default for "what's new to listen to")
- Caveat: limit=0 returns 0 results; always pass limit>0. No total count in response.
- No cross-library endpoint exists; must call once per podcast library.
