---
name: Book Media Shapes (minified vs basic vs expanded)
description: Exact field lists for Book media JSON across the three serialization variants, plus ebookFile shape and which endpoint returns which variant
type: reference
---

Source: `server/models/Book.js` (Sequelize model, current — the old `server/objects/Book.js` plain-object class no longer exists in the DB-backed rewrite). Confirmed against server v2.34.0 both by source reading and live requests to a real "The Way of Kings" item that has both audio tracks and a PDF ebookFile.

## Three distinct Book media serializations — not just two
1. **`toOldJSONMinified()`** — used in library item LISTS (`GET /api/libraries/:id/items`, always, regardless of `?minified=` query param — that param controls something else internally, not this choice of serializer per `LibraryItem.getByFilterAndSort` in `server/models/LibraryItem.js`).
2. **`toOldJSON()`** ("basic") — used by `GET /api/items/:id` **without** `?expanded=1`. This is NOT the minified shape and NOT the expanded shape — it's a third, distinct shape.
3. **`toOldJSONExpanded(libraryItemId)`** — used by `GET /api/items/:id?expanded=1`.

Router: `server/controllers/LibraryItemController.js` `findOne()` — `req.query.expanded == 1` branches to `toOldJSONExpanded()`, otherwise plain `toOldJSON()`. Minified is never an option on the single-item detail endpoint.

## Field lists (media object only)

**Minified** (`oldMetadataToJSONMinified` + flat counts):
`id, metadata{title,titleIgnorePrefix,subtitle,authorName,authorNameLF,narratorName,seriesName,genres,publishedYear,publishedDate,publisher,description,isbn,asin,language,explicit,abridged}, coverPath, tags, numTracks, numAudioFiles, numChapters, duration, size, ebookFormat`
- metadata here is FLATTENED (authorName/seriesName as pre-joined strings, no arrays of author/series objects).
- `ebookFormat` is a flat string (e.g. `"pdf"`), not an object — pulled from `this.ebookFile?.ebookFormat`.

**Basic** (`toOldJSON`, i.e. detail endpoint without `?expanded=1`):
`id, libraryItemId, metadata{title,subtitle,authors[{id,name}],narrators[],series[{id,name,sequence}],genres,publishedYear,publishedDate,publisher,description,isbn,asin,language,explicit,abridged}, coverPath, tags, audioFiles[], chapters[], ebookFile{...}`
- metadata here uses `oldMetadataToJSON()` — structured authors/series arrays, no authorName/seriesName/titleIgnorePrefix/descriptionPlain strings.
- Full `audioFiles[]` and `ebookFile` object included, but NO `duration`, `size`, or `tracks` convenience fields, and NO `numTracks`/`numAudioFiles`/`numChapters`/`ebookFormat` flat fields.

**Expanded** (`toOldJSONExpanded`, `?expanded=1`):
`id, libraryItemId, metadata{...+titleIgnorePrefix,authorName,authorNameLF,narratorName,seriesName,descriptionPlain}, coverPath, tags, audioFiles[], chapters[], ebookFile{...}, duration, size, tracks[]`
- metadata uses `oldMetadataToJSONExpanded()` — has BOTH the structured authors/series arrays AND the flattened authorName/seriesName strings AND `descriptionPlain` (HTML-stripped description).
- Adds `duration`, `size`, and `tracks[]` (the playable AudioTrack list with `contentUrl`/`startOffset` computed via `getTracklist(libraryItemId)`).
- **Does NOT include `numTracks`, `numAudioFiles`, `numChapters`, or flat `ebookFormat`** — those are minified-only convenience fields. Clients needing counts on the expanded/basic shape must derive them (`audioFiles.length`, `chapters.length`, `ebookFile?.ebookFormat`).

## `numMissingParts` / `numInvalidAudioFiles` — DO NOT EXIST
Confirmed via `grep -rn "numMissingParts|numInvalidAudioFiles" server/ client/` → zero hits anywhere in current server or client source (v2.34.0). These are not present on ANY Book media serialization (minified, basic, or expanded). If they ever existed it was in a pre-Sequelize-rewrite version; do not rely on them. The nearest analogues live on the LibraryItem level, not media: `isMissing`, `isInvalid` (booleans on the LibraryItem itself, in all three LibraryItem serializations).

## ebookFile object shape (identical on basic and expanded — confirmed byte-identical live)
Source: `server/objects/files/EBookFile.js` `toJSON()`:
```
ebookFile: {
  ino: string,
  metadata: { filename, ext, path, relPath, size, mtimeMs, ctimeMs, birthtimeMs },  // FileMetadata.toJSON()
  ebookFormat: string,   // e.g. "pdf", "epub" — falls back to metadata.format if not set explicitly
  addedAt: number,       // epoch ms
  updatedAt: number      // epoch ms
}
```
`ebookFile.ebookFormat` is the canonical field for format — NOT nested under `ebookFile.metadata`. `EBookFile.isEpub` getter (`ebookFormat === 'epub'`) exists server-side but is not serialized.

## Live-confirmed example (server v2.34.0, item "The Way of Kings", has audio + PDF ebook)
Minified media top-level keys: `id, metadata, coverPath, tags, numTracks, numAudioFiles, numChapters, duration, size, ebookFormat`
Basic media top-level keys: `id, libraryItemId, metadata, coverPath, tags, audioFiles, chapters, ebookFile`
Expanded media top-level keys: `id, libraryItemId, metadata, coverPath, tags, audioFiles, chapters, ebookFile, duration, size, tracks`
ebookFile.ebookFormat = `"pdf"` on both basic and expanded, identical object.

See also [[podcast-model]] for the podcast-side equivalents (Podcast/PodcastEpisode have their own minified/expanded rules, different field names).
