---
name: scanner-metadata-parsing
description: How the ABS library scanner reads metadata.json, folder names, and embedded audio/chapter tags for books and podcasts, and the precedence order between them
type: reference
---

Source: `~/OpenSource/audiobookshelf`, verified at HEAD `e39e8d8cf26a5f9c17f04e94c4863fa95b28cec3` (2026-05-08).

## Books

- `metadata.json` (literal filename only; legacy `metadata.abs` getter exists at `LibraryItemScanData.js:156-159` but is dead code — nothing calls it) is read unconditionally on every scan via `AbsMetadataFileScanner.scanBookMetadataFile` (`server/scanner/AbsMetadataFileScanner.js:18-47`). **`storeMetadataWithItem` server setting does NOT gate reading** — it only gates where the server *writes* metadata.json back (item folder vs `<MetadataPath>/items/<id>/`, see `BookScanner.js:815-824`).
- Flat JSON keys (see `Book.getAbsMetadataJson()` server/models/Book.js:339-361): `tags[]`, `chapters[{start,end,title}]`, `title`, `subtitle`, `authors[]` (plain strings), `narrators[]`, `series[]` (strings like `"Name #1"`, sequence parsed via regex ` #([^#\s]+)$` in `parseSeriesString.js:18`), `genres[]`, `publishedYear`, `publishedDate`, `publisher`, `description`, `isbn`, `asin`, `language`, `explicit`, `abridged`.
- Precedence is per-library, configurable via `Library.settings.metadataPrecedence`, default order `['folderStructure','audioMetatags','nfoFile','txtFiles','opfFile','absMetadata']` (`server/models/Library.js:84-86`) — later wins. **metadata.json (absMetadata) wins by default over embedded ID3 tags and folder-name parsing.** Sequential dispatch in `BookScanner.js:673-807`.
- Folder naming: `Author/Series/Title/` (top 3 levels), parsed in `server/utils/scandir.js` `getBookDataFromDir` (scandir.js:149-173). Sub-parsers on the title-folder string (in order): ASIN bracket `[B0015T963C]` (scandir.js:265-275), trailing `{Narrator Name}` (scandir.js:182-186), sequence prefix `Vol 01 -` / `Book 2 -` / `1.` (scandir.js:191-207, only if series dir present), leading year `1980 - Title` (scandir.js:234-245), subtitle after first ` - ` (only if `scannerParseSubtitle` setting on, scandir.js:253-257,308).
- Cover filename: only literal `cover.<ext>` is special-cased (regex `/\/cover\.[^.\/]*$/`, BookScanner.js:172/686); otherwise falls back to first image file found. No `folder.jpg`/`poster.jpg` support. `SupportedImageTypes = ['png','jpg','jpeg','webp']` (globals.js:2).

## Podcasts

- Podcast-level `metadata.json` via `AbsMetadataFileScanner.scanPodcastMetadataFile` (AbsMetadataFileScanner.js:57-82), also unconditional/not gated by storeMetadataWithItem. Keys (`Podcast.getAbsMetadataJson()` server/models/Podcast.js:187-202): `tags[]`, `title`, `author`, `description`, `releaseDate`, `genres[]`, `feedURL`, `imageURL`, `itunesPageURL`, `itunesId`, `itunesArtistId`, `language`, `explicit`, `podcastType` ('episodic'|'serial'). Note key casing: `feedURL`/`imageURL`/`itunesPageURL`, not `feedUrl`/`imageUrl`.
- Podcast metadata precedence is NOT configurable (metadataPrecedence only applies to book libraries): audio tags from first episode applied first (`PodcastScanner.js:393-395`), then metadata.json overwrites (`PodcastScanner.js:397-398`) — metadata.json always wins.
- Episode-level fields come only from embedded audio tags (no per-episode JSON file), mapped in `AudioFileScanner.setPodcastEpisodeMetadataFromAudioMetaTags` (AudioFileScanner.js:419-487): `tagComment`/`tagDescription`→description, `tagSubtitle`→subtitle, `tagDate`→pubDate+publishedAt, `tagDisc`→season, `tagTrack`/`tagSeriesPart`→episode, `tagTitle`→title, `tagEpisodeType`→episodeType (must be `full`/`trailer`/`bonus` or rejected).
- Cover filename convention identical to books (`cover.*` then first image fallback), PodcastScanner.js:189-192/305-308.

## Audio file validity (ffprobe)

- `AudioFileScanner.scan()` (AudioFileScanner.js:156-180) calls `prober.probe()` (server/utils/prober.js, ffprobe wrapper, invoked prober.js:289-317; `FFPROBE_PATH` env overrides binary).
- Corrupt/0-byte files: ffprobe error → `probe()` returns `{error}` (prober.js:294-299); no audio/video stream → explicit error (prober.js:309-313). Either way `AudioFileScanner.scan()` returns `null` (AudioFileScanner.js:159-167) and nulls are filtered out (`.filter(sr => sr)`, AudioFileScanner.js:197) — **file silently dropped from track list**, only an error-level log emitted, not marked invalid on a partial object.
- No explicit `duration > 0` gate — only requires an audio stream to exist; `duration` can be `null` and file still becomes a valid track.
- Extension allow-list (`globals.SupportedAudioTypes`, globals.js:3) only decides what gets queued for probing at all, not accept/reject — real gate is ffprobe stream presence.
- If item ends up with zero valid audio files (and no ebook for books): item skipped on new scan (BookScanner.js:452-456) or marked `isMissing=true` on rescan (BookScanner.js:396-407); podcasts: ignored on new scan (PodcastScanner.js:268-272).

## Embedded chapters (m4b etc.)

- Read via the same ffprobe call — `prober.js` `parseChapters()` (prober.js:139-162) consumes ffprobe's `data.chapters`, extracts title (chap['TAG:title']→chap.title→chap.tags.title fallback), start/end (prefers `*_time` strings, else raw/time_base).
- Book-level assembly: `AudioFileScanner.getBookChaptersFromAudioFiles` (AudioFileScanner.js:495-574), part of the `audioMetatags` precedence stage (so a `metadata.json` `chapters` key can still override per the precedence chain above). Priority: (1) Overdrive Media Markers if present, (2) embedded ffprobe chapters (verbatim if single file / identical across files, else concatenated with cumulative offsets, dropping any chapter <0.1s), (3) synthesized one-chapter-per-file fallback for multi-file books with no embedded chapters.

Full investigation with example synthetic `metadata.json` JSON schemas for books and podcasts was returned to the user in the 2026-08-21 conversation — regenerate by re-running this scanner investigation if the schemas are needed again and this note isn't enough.

Related: [[podcast-model]] (server-side podcast/episode API shapes — this note covers scanner ingestion, that one covers API serialization).
