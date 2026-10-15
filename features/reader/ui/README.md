# Reader UI

The e-book reader is a Compose screen around a platform web view (`BookRenderer`: Android
`WebView`, iOS `WKWebView`) running [foliate-js](https://github.com/johnfactotum/foliate-js). The
page and the book are served from the app itself, never the network.

## Bundled web code (`src/commonMain/composeResources/files/reader/`)

| Path | Source | License |
|---|---|---|
| `index.html`, `reader.js` | Campfire | GPL-3.0-only |
| `foliate/` | foliate-js `78914aef4466eb960965702401634c2cb348e9b1` (2026-05-01), the files the EPUB, MOBI/AZW3, FB2 and CBZ paths load; no PDF (pdf.js), TTS or search. One local change, marked `Campfire:` in `paginator.js`: a `max-column-count-portrait` attribute | MIT (`foliate/LICENSE`) |
| `fonts/` | Literata, Atkinson Hyperlegible Next and OpenDyslexic from Fontsource 5.3.0: Latin subset, regular and bold, upright and italic | SIL OFL 1.1 (`fonts/LICENSE-*.txt`) |
| `epubjs-cfi/epubcfi.js` | epub.js 0.3.88 `src/epubcfi.js`, unmodified except its import line | BSD-2-Clause (`epubjs-cfi/LICENSE`) |
| `epubjs-cfi/core.js` | the four helpers `epubcfi.js` uses from epub.js `src/utils/core.js` | BSD-2-Clause |

To update foliate-js, copy the same files from the new commit, reapply the `Campfire:` change and
update the hash above.

## Reading positions

Positions are saved in the format the official Audiobookshelf readers use, so they carry over
between clients: an epub.js CFI for EPUB, a 1-based page number for CBZ. foliate-js counts text
nodes differently from epub.js after inline elements (about 4% of positions), so `reader.js`
writes and resolves EPUB CFIs with epub.js's own `EpubCFI` rather than foliate's.
