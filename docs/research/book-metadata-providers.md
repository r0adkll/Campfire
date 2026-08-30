# Third-party book metadata & review provider APIs

Research for integrating one or more external providers behind an abstraction in Campfire, for three
use cases:

1. **Reviews** — community star ratings and written review text for a book
2. **Series-gap detection** — given a series the user has, detect missing and upcoming/unreleased books
3. **Supplemental metadata** — covers, descriptions, genres, author info, release dates

All claims below were verified against primary sources (official docs, developer portals, source
code, live API calls) on **2026-08-29**; each claim carries its source URL. Anything that could not
be traced to a primary source is marked **UNVERIFIED**. This file lives in `docs/research/` — a new
directory created for this document (the repo previously had `docs/agents/`, `docs/architecture/`,
`docs/adr/` but no research home).

---

## Executive summary

- **Hardcover is the only viable provider for reviews/ratings and the best for series-gap
  detection.** Its GraphQL API exposes average rating, ratings distribution, review text, and series
  positions *including unreleased future books* (verified live). The catch: auth is a per-user
  Personal Access Token the user must generate and paste (OAuth for external apps is on their 2026
  roadmap), and docs say tokens must be kept out of browsers/shared clients.
- **Goodreads is dead as an API** (no new keys since 2020-12-08, endpoints redirect to the
  homepage) and its ToS explicitly prohibits scraping. **StoryGraph has no API** (roadmap item,
  "long-term", no ETA). **LibraryThing's APIs are disabled** and its developer pages are
  Cloudflare-gated even to simple HTTP clients.
- **Open Library** is a solid keyless, openly-licensed fallback for covers, descriptions, and
  aggregate ratings (no review text), but its series model is a free-text string on editions — not
  usable for gap detection.
- **Google Books** offers `averageRating`/`ratingsCount` with an embeddable API key but no review
  text, no documented series model, undocumented quotas, and heavy branding/attribution ToS that fit
  poorly inside a native app.
- **For audiobook-native metadata**, Audnexus (keyless, Audible-derived: description, genres,
  Audible rating, author bios, chapters) is the proven ABS-ecosystem choice, and its authors now
  point new projects to its successor **AudiobookDB** (API-key REST, includes series and ratings
  endpoints, still young). Campfire can also piggyback the ABS server's own metadata providers /
  custom-provider plugin contract, but that channel carries no ratings or reviews at all.
- **Recommended shape**: a provider abstraction with (a) Hardcover as the premium per-user-token
  provider covering all three use cases, (b) keyless fallbacks — Open Library for print-side
  metadata/aggregate ratings, Audnexus/AudiobookDB for audiobook metadata, and (c) skip Goodreads,
  StoryGraph, LibraryThing, and ISBNdb entirely.

## Comparison table

| Provider | API style / auth | Review text | Rating aggregates | Series order + upcoming | Metadata (cover/desc/genre/author) | Limits / price | Licensing & ToS gotchas | Fit: Reviews / Series / Metadata |
|---|---|---|---|---|---|---|---|---|
| **Hardcover** | GraphQL; per-user PAT (Bearer), scoped + expiring; OAuth "2026 roadmap" | ✅ `user_books.review` | ✅ `rating`, `ratings_count`, `ratings_distribution` | ✅ float positions, `compilation` flag, `is_completed`, unreleased entries (verified) | ✅ incl. author bios, ISBN/ASIN mappings | Free: 5k/day, burst 10, 60/min; ≤5 top-level queries/request | No user-owned data in commercial products except on the user's behalf; no browser use; beta may reset tokens | 🟢 / 🟢 / 🟢 |
| **Goodreads** | Retired REST; no new keys since 2020-12-08 | ❌ | ❌ | ❌ | ❌ | n/a | ToS bans "data mining, robots, or similar" tools | 🔴 / 🔴 / 🔴 |
| **StoryGraph** | No API (roadmap, no ETA) | ❌ | ❌ | ❌ | ❌ | n/a | n/a | 🔴 / 🔴 / 🔴 |
| **Open Library** | REST/JSON; no auth (identified User-Agent) | ❌ (no review feature) | ✅ aggregate only (`ratings_average`, distribution) | 🔴 free-text `series` string on editions, no ordering, no upcoming | ✅ covers, desc, subjects, authors | 1 rps (3 rps identified); covers by ISBN 100/IP/5 min | Archive.org asserts no proprietary rights; provenance mixed | 🟡 / 🔴 / 🟢 |
| **Google Books** | REST; API key (public data) | ❌ | ✅ `averageRating`, `ratingsCount` | 🔴 `seriesInfo` not in public reference | 🟡 covers/desc/categories | Quotas undocumented (console-set; ~1k/day default UNVERIFIED) | "Powered by Google" attribution, must link out, may not reorder results, no charging users | 🟡 / 🔴 / 🟡 |
| **LibraryThing** | "APIs currently disabled until further notice"; endpoints Cloudflare-blocked | ❌ | ❌ | ❌ | ❌ | n/a | member-book data withheld due to Amazon licensing | 🔴 / 🔴 / 🔴 |
| **ISBNdb** | REST; paid API key | ❌ | ❌ | ❌ (no series data point) | ✅ 19 data points incl. synopsis, cover | $14.99–$99.99/mo; 5k–50k/day, 1–5 rps | Commercial DB; standard paid terms | 🔴 / 🔴 / 🟡 |
| **Wikidata** | SPARQL/REST; no auth | ❌ | ❌ | 🟡 P179 + P1545 ordinals, good model but patchy book coverage | 🟡 authors strong, covers weak | WDQS: 60 s timeout, 5 parallel/IP, UA policy | CC0 — no restrictions | 🔴 / 🟡 / 🟡 |
| **Inventaire** | REST (OpenAPI); no auth for reads | ❌ | ❌ | 🟡 Wikidata-centered series entities | 🟡 | not documented | CC0 data, AGPL code | 🔴 / 🟡 / 🟡 |
| **BookBrainz** | REST, **alpha** | ❌ | ❌ | 🟡 entity model exists, tiny dataset | 🟡 | not documented | CC0-family open data (MetaBrainz) | 🔴 / 🔴 / 🔴 |
| **Audnexus** | REST; no auth | ❌ | ✅ Audible rating value | 🔴 no series endpoints | ✅ audiobook-native: desc, genres, author bios, chapters | none documented | GPLv3 service; data is Audible-derived | 🟡 / 🔴 / 🟢 |
| **AudiobookDB** | REST (OpenAPI 3.1); API key or cookie session | UNVERIFIED (Ratings endpoints exist) | ✅ Ratings group | 🟡 Series endpoints exist; young dataset | ✅ audiobook-native, ASIN resolution | Two-tier: per-request bucket + daily cost-unit budget | Spec labeled "AudiobookDB Proprietary"; see their terms | 🟡 / 🟡 / 🟢 |
| **Audible (unofficial)** | REST, reverse-engineered | 🟡 `reviews` response_group | ✅ `rating` group | 🟡 `series` group | ✅ | unknown | Unofficial, undocumented, breakage/ToS risk | 🔴 / 🟡 / 🟡 |

Legend: 🟢 good fit · 🟡 partial/with caveats · 🔴 not viable.

---

## 1. Hardcover (hardcover.app)

**API**: GraphQL at `https://api.hardcover.app/v1/graphql` — "exactly the same API used by the
website, iOS and Android apps," currently in beta. Docs: https://docs.hardcover.app/api/getting-started/
(the docs site 403s non-browser clients; content verified from its open-source repo:
https://github.com/hardcoverapp/hardcover-docs/blob/main/src/content/docs/api/Getting-Started.mdx —
all Hardcover doc claims below cite this file unless noted).

**Auth**:
- Per-user **Personal Access Tokens** created at the user's account API page; each token gets a
  label, an **expiration period**, and (since **August 2026**) **scoped permissions** — pre-Aug-2026
  tokens behave as `all` scope. Sent as `Authorization: Bearer <token>`.
- A **PAT Link Builder** (https://docs.hardcover.app/api/pat-link-builder/) lets a third-party app
  generate a link that pre-checks the scopes it needs — the intended onboarding flow for apps like
  Campfire today.
- **No OAuth yet**: roadmap says "2026: OAuth support will be added for external applications" and
  "2026: Client-side API use will be supported."
- Beta caveats: "We may reset tokens without notice while in beta"; "Don't share your token!
  Someone could delete your account with it."
- Restriction to note: "This should only be used from a code backend — never from a browser… You can
  only access this API from localhost or APIs," and "Queries are not allowed to run in the browser,
  they must be run in an environment where the token can be kept secure." A native KMP app is not a
  browser and the token is the user's own, but the *spirit* is token secrecy — the planned
  "client-side API use" roadmap item is the sanctioned path for app-embedded use. Worth confirming
  with Hardcover (Discord) before shipping. **UNVERIFIED** whether they consider a native mobile
  client acceptable pre-OAuth; the entire third-party sync ecosystem (see §10) already works this
  way with user-pasted tokens.

**Rate limits** (same source):
- `Free`: 5,000 requests/day, burst 10, 60/min. `Supporter`: 50,000/day, burst 15, 60/min.
- Hard cap of **5 top-level queries per request** (1 for `search`); 30 s query timeout, 2 s search
  timeout; `_like`/`_regex`-family predicates disabled.
- IETF-draft `RateLimit`/`RateLimit-Policy` headers plus legacy `X-RateLimit-*` and `Retry-After`.
- Higher limits by request for public-good projects; a commercial tier is "being built out right now."

**Data** (schema docs in the same repo under `src/content/docs/api/GraphQL/Schemas/`):
- `books`: `rating` (average 0–5), `ratings_count`, `ratings_distribution` (per-star jsonb),
  `reviews_count`, `release_date`, `alternative_titles`, `audio_seconds`, `cached_image`,
  `book_mappings` ("External platform mappings"), `book_series`
  (https://github.com/hardcoverapp/hardcover-docs/blob/main/src/content/docs/api/GraphQL/Schemas/Books.mdx).
- `series`: `name`, `description`, `books_count`, **`is_completed`** ("Series is complete"),
  `identifiers` (external ids), canonical/duplicate handling
  (…/Schemas/Series.mdx).
- `book_series`: **`position` (float8)**, `details` (text form of position), **`compilation`**
  ("is a compilation of books in the series") — the key fields for ordering and for filtering box
  sets (…/Schemas/BookSeries.mdx).
- `user_books`: `rating`, **`review` (text)**, `review_markdown`, `review_has_spoilers`,
  `has_review`, reading statuses (…/Schemas/UserBooks.mdx).
- `authors`: bios, aliases/pen names, alternate names (…/Schemas/Authors.mdx); `editions` carry
  `isbn_10`, `isbn_13`, publisher, release date (…/Schemas/Editions.mdx and the
  Getting-Started examples).

**Series quality — verified live** (query against `api.hardcover.app` via the Hardcover API,
2026-08-29, series `the-stormlight-archive`): positions are floats (`0.1` for *The Way of Kings
Prime*, `1.1/1.2` for split-part audio editions), and the series **includes unreleased/announced
books**: *Horneater* at position 4.5 with `release_year: 2027`, and "Untitled Stormlight Archive
#6…#10" at positions 6–10 with placeholder future years (2031–2035). This is exactly what
series-gap/upcoming detection needs. Caveat: the same positions also carry translated editions and
box sets as separate `books` rows (e.g. Russian/Polish/French translations all at position 1), so a
client must filter — `compilation`, `users_count`, and title/language heuristics work.

**Licensing / ToS** (Getting-Started "Commercial API Use" section):
- "We make no copyright or proprietary rights over this data."
- Commercial products / publicly accessible sites **may not use data owned by users** (libraries,
  reviews, ratings, dates read, lists, goals…) "unless on behalf of a user who has allowed their
  data to be accessed." A client app acting with the user's own token is the sanctioned pattern.
- "Using aggregate data is allowed, if it's mentioned to be Hardcover data (ex: … average Hardcover
  rating)" — i.e. **attribution required when showing aggregate ratings**.
- API may not be used "to train large language models that are made publicly available or used
  commercially."
- Warning: user-uploaded cover images ⇒ publish a DMCA takedown policy if re-exposing them publicly
  (less relevant for an in-app client).

**Fit**: Reviews 🟢 (only provider with queryable review text + distribution); Series 🟢 (verified
upcoming-book data, positions, `is_completed`); Metadata 🟢. Cost: every user needs a Hardcover
account + pasted PAT until OAuth lands.

## 2. Goodreads

- **API retired.** `https://www.goodreads.com/api`, `/api/documentation`, and `/api/terms` all now
  302-redirect to the goodreads.com homepage (observed directly, 2026-08-29).
- Goodreads' own developer forum thread records the announcement: "As of December 8th 2020,
  Goodreads is no longer issuing new developer keys for our public developer API and plans to retire
  these tools" — https://www.goodreads.com/topic/show/21788520-api-deprecation. Users there also
  report existing keys being deactivated after 30 days of inactivity. The help-center article
  (https://help.goodreads.com/s/article/Does-Goodreads-support-the-use-of-APIs) exists but renders
  only via JavaScript, so its exact current wording is **UNVERIFIED** here.
- **Scraping is ToS-prohibited.** Goodreads Terms of Use: the license "does not include … any
  collection and use of any book listings, descriptions, reviews or other material included in the
  Service … or any use of data mining, robots, or similar data gathering and extraction tools" —
  https://www.goodreads.com/about/terms.
- Alternatives: Amazon's Product Advertising API (https://webservices.amazon.com/paapi5/documentation/)
  is affiliate-sales tooling requiring an Associates account with qualifying sales, not a
  book-review data source — not a practical channel for Campfire (assessment). Per-user RSS shelf
  feeds reportedly still function but are per-user, undocumented, and cover only that user's shelves
  (**UNVERIFIED** — no primary doc exists).
- **Fit**: not viable for any use case.

## 3. The StoryGraph

- **No public API exists.** "An API" is an item on their official roadmap, categorized long-term,
  posted 2021-03-14: https://roadmap.thestorygraph.com/features/posts/an-api. Team response (Nadia):
  "There isn't an ETA … a team of one app/web developer at the moment and this isn't a priority."
- **Fit**: not viable; re-check the roadmap occasionally.

## 4. Open Library (openlibrary.org)

- **APIs**: Search, Works & Editions ("Books"), Authors, Subjects, Covers, Lists, My Books, Search
  Inside — indexed at https://openlibrary.org/developers/api with per-API docs under
  `/dev/docs/api/*`. REST/JSON, stable, well documented.
- **Auth/limits**: none for reads; default **1 request/sec**, or **3 requests/sec** for "identified"
  requests carrying a `User-Agent` with app name + contact email
  (https://openlibrary.org/developers/api). Covers API: cover lookups "by ids other than CoverID and
  OLID are rate-limited … only 100 requests/IP … every 5 minutes," 403 beyond that
  (https://openlibrary.org/dev/docs/api/covers). Cover URL pattern
  `https://covers.openlibrary.org/b/$key/$value-$size.jpg` (key ∈ ISBN, OCLC, LCCN, OLID, ID).
- **Ratings**: aggregate only. Live-verified 2026-08-29:
  `GET https://openlibrary.org/works/OL45804W/ratings.json` →
  `{"summary":{"average":3.94,"count":120,…},"counts":{"1":11,…,"5":56}}`, and the Search API
  returns `ratings_average`, `ratings_count`, per-star `ratings_count_N`, `already_read_count` as
  requestable fields (`https://openlibrary.org/search.json?...&fields=ratings_average,...`). Note
  there is **no `/dev/docs/api/ratings` page** (404) — the ratings endpoints work but aren't in the
  official API index. **No written-review text**: Open Library has no review-hosting feature to
  expose (assessment from the API surface; no reviews API is listed at
  https://openlibrary.org/developers/api).
- **Series — weak, confirmed**: there is no series entity or series API; series info is a free-text
  edition field. Live-verified: `https://openlibrary.org/books/OL7826547M.json` →
  `"series": ["A Song of Ice and Fire #1"]`. No ordering semantics, no completeness, no upcoming
  books. Useless for gap detection.
- **Licensing**: "Internet Archive does not assert any new copyright or other proprietary rights
  over any of the material in the Open Library database," while noting possible upstream rights
  issues in some records — https://openlibrary.org/developers/licensing. Monthly bulk dumps
  available; APIs are "not intended to serve as a bulk data backend"
  (https://openlibrary.org/developers/api, dumps at https://openlibrary.org/developers/dumps).
- **Fit**: Reviews 🟡 (aggregates only), Series 🔴, Metadata 🟢 (keyless, open, good covers —
  mind the 100/5-min ISBN-cover limit; resolve to CoverID/OLID first).

## 5. Google Books API

- **API**: REST `volumes` search/get; public-data requests need an **API key** (or OAuth token);
  private bookshelf data needs OAuth — https://developers.google.com/books/docs/v1/using.
- **Ratings**: `volumeInfo.averageRating` ("mean review rating … min = 1.0, max = 5.0") and
  `volumeInfo.ratingsCount` are documented —
  https://developers.google.com/books/docs/v1/reference/volumes. **No community review text** is
  exposed: `volumeInfo` has no review field; the only review resource is the authenticated user's own
  `mylibrary` review (same reference page).
- **Series**: `seriesInfo` is **not documented** in the public Volumes reference (verified against
  https://developers.google.com/books/docs/v1/reference/volumes); any `seriesInfo` seen in responses
  is an undocumented/unsupported surface. No series ordering or completeness API. 🔴 for gap
  detection.
- **Quotas**: not stated in public docs (the `using` page has none). Per-project quotas appear in the
  Google Cloud console; community reports put the default around 1,000 requests/day —
  **UNVERIFIED** from any Google-owned public page.
- **ToS/branding**: "You may not charge users any fee for the use of your application" without
  Google's permission (https://developers.google.com/books/terms — relevant to Campfire if it ever
  charges); branding rules require Google attribution ("powered by Google") when displaying results,
  a prominent link to the Google Books page for each result, and forbid reordering/altering
  API-returned results (https://developers.google.com/books/branding). These are awkward
  requirements inside a native app UI.
- **KMP shipping**: an API key can be embedded in the app (it's designed for client use with key
  restrictions), but the quota is shared across all installs and keys are extractable.
- **Fit**: Reviews 🟡 (aggregate only, attribution strings required), Series 🔴, Metadata 🟡
  (fine as a fallback; ABS server already exposes it as a provider anyway).

## 6. LibraryThing

- **Status: APIs disabled.** LibraryThing's developer hub (https://www.librarything.com/services/)
  and wiki (https://wiki.librarything.com/index.php/LibraryThing_APIs) currently state the APIs
  are disabled until further notice, and that no member-books API is offered due to data-licensing
  restrictions from external providers, "especially Amazon" (wording as surfaced by search engines
  indexing those pages; the pages themselves sit behind a Cloudflare browser check and returned
  403/JS-challenge to every non-browser client tried — so the exact current phrasing is
  **UNVERIFIED**, though the block itself was observed directly on 2026-08-29).
- **thingISBN**: the endpoint (`https://www.librarything.com/api/thingISBN/<isbn>`) returns the
  Cloudflare challenge page to non-browser clients (observed live, 2026-08-29) — i.e. even the one
  historically open API is not programmatically reachable today. Member discussion of the outage:
  https://www.librarything.com/topic/320927 ("What happened to the APIs?").
- **Fit**: not viable for any use case.

## 7. ISBNdb

- **API**: REST at `https://api2.isbndb.com`, API key sent as an `Authorization` header (query-param
  auth rejected) — https://isbndb.com/apidocs/v2 (v2.7.1). 429s on per-minute and daily plan quotas.
- **Pricing** (https://isbndb.com/isbn-database): Basic **$14.99/mo** — 5,000 daily calls, 1 call/s;
  Premium **$35.99/mo** — 15,000 daily, 3 calls/s, 1,000-result bulk; Pro **$99.99/mo** — 50,000
  daily, 5 calls/s, real-time pricing; annual discounts; academic plan $7.50/mo.
- **Data**: "up to 19 data points per book including ISBN10, ISBN13, title, author, publication
  date, publisher, binding, pages, list price, cover thumbnail, language, edition, format, synopsis,
  subject, weight, and dimensions" (same page). **No reviews, no ratings, and no series field** in
  the advertised data points.
- **Fit**: Reviews 🔴, Series 🔴, Metadata 🟡 (paid, ISBN-keyed print metadata — redundant with what
  ABS + Open Library already give Campfire for free).

## 8. Inventaire / Wikidata / BookBrainz

**Wikidata**
- **License**: "All structured data in the main, property and lexeme namespaces is made available
  under the Creative Commons CC0 License" — https://www.wikidata.org/wiki/Wikidata:Licensing.
- **Series model**: books link to series via **P179 "part of the series"** with qualifier
  **P1545 "series ordinal"** ("position of an item in its parent series," to be "used as a qualifier
  to P179") — https://www.wikidata.org/wiki/Property:P1545. Structurally the cleanest open series
  model; supports fractional/odd ordinals as strings ("3.5", "1b").
- **Access**: REST/Action APIs and the SPARQL Query Service, no auth; WDQS limits: 60 s hard query
  deadline, 60 s processing per 60 s window, 5 parallel queries per IP, User-Agent policy enforced —
  https://www.mediawiki.org/wiki/Wikidata_Query_Service/User_Manual.
- **Caveat**: coverage is notability-driven — strong for famous series/authors, patchy for midlist
  and audiobook-first titles; no ratings/reviews; matching from ISBN/ASIN is hit-or-miss
  (assessment).

**Inventaire**
- REST API described by OpenAPI 3.1 at https://api.inventaire.io/ (server source:
  https://codeberg.org/inventaire/inventaire/). Bibliographic ("entities") data is **CC0** and
  Wikidata-centered — https://wiki.inventaire.io/wiki/Entities_data; dumps at
  https://data.inventaire.io/ and a SPARQL service at https://query.inventaire.io/. No published
  rate limits found. Community-scale data maturity; no ratings/reviews.

**BookBrainz**
- "The web service is the primary way of getting BookBrainz data" but the API is in **alpha**
  (docs at https://api.test.bookbrainz.org/1/docs/); weekly SQL dumps —
  https://bookbrainz.org/develop. Dataset is small and the API isn't production-hosted; not ready
  as a dependency (assessment).

- **Fit (all three)**: Reviews 🔴; Series 🟡 (Wikidata only, as a free enrichment source, not a
  primary); Metadata 🟡 (author data from Wikidata is genuinely good and CC0).

## 9. Audiobook-specific providers

### Audnexus (audnex.us)

- **API**: public REST at `https://api.audnex.us` — authors (search by name, get by ASIN: biography,
  genres, similar authors), books by ASIN (title, authors, narrators, release date, **rating**,
  runtime, description, genres/tags, cover), chapters by ASIN; multi-region (us, uk, de, fr, …) —
  https://audnex.us/ (Redocly reference). **No auth, no documented rate limits.** GPLv3, source:
  https://github.com/laxamentumtech/audnexus.
- **Data source**: "An audiobook data aggregation API, combining multiple sources" — in practice
  Audible: self-hosting requires "Registered Audible device keys, ADP_TOKEN and PRIVATE_KEY, for
  chapters" via the community `audible` Python tooling (README). So its legitimacy rests on the same
  unofficial-Audible ground as §Audible below, but laundered through a community-run service that
  Audiobookshelf itself ships as a built-in provider (see §ABS).
- **Succession notice** (README, current): "**Looking for audiobook metadata? Check out
  [AudiobookDB](https://audiobookdb.org)** — our community-maintained audiobook metadata database
  and the successor to this API … New projects should start there; audnexus remains online and
  maintained."
- **Fit**: Reviews 🟡 (a single Audible rating value, no text), Series 🔴, Metadata 🟢 for
  audiobooks (keyless, ASIN-keyed, chapters!).

### AudiobookDB (audiobookdb.org) — Audnexus's successor

- **API**: REST at `https://audiobookdb.org/api`, OpenAPI 3.1 spec v1.0.0 at
  https://audiobookdb.org/docs/api. Endpoint groups: Audiobooks (composite create/resolve —
  including "resolve an external identifier (e.g. Audible ASIN) to its catalog entry"), Books,
  Releases, Search, **Series**, People, Publishers, Genres, Tags, **Ratings**, Images, Users.
- **Auth**: API keys or cookie sessions; some operations are cookie-only; writes go through
  community moderation ("other contributors queue a moderation proposal and receive 202").
- **Rate limits**: "two-tier: a short-term per-request bucket (advertised by the RateLimit-*
  headers) and a daily cost-unit budget," with per-endpoint cost classes (detail=1, list=2,
  search=3, write=5, bulk=10) (same docs page).
- **Caveats**: young project (the public homepage's catalog counters rendered as zero to a
  non-JS client on 2026-08-29 — dataset scale **UNVERIFIED**); the OpenAPI spec is labeled
  "AudiobookDB Proprietary," and reuse terms live in their Terms page (not independently reviewed —
  **UNVERIFIED**).
- **Fit**: promising 🟢 metadata / 🟡 series+ratings candidate for the audiobook side; watch its
  maturity before depending on it.

### Audible unofficial API (api.audible.com)

- Community-reverse-engineered documentation lives in the `audible` Python project: "There is
  currently no publicly available documentation about the Audible API" —
  https://audible.readthedocs.io/en/latest/misc/external_api.html. `GET /1.0/catalog/products`
  supports `response_groups` including **`rating`, `reviews`, `review_attrs`, `series`**,
  `contributors`, `media`, `product_desc` (same page). "Most calls need to be authenticated";
  which catalog calls work anonymously is **UNVERIFIED**.
- Unofficial, undocumented, and presumably contrary to Amazon's Conditions of Use — the same posture
  as Goodreads scraping. If Audible data is wanted, consume it via Audnexus/AudiobookDB (or the ABS
  server's Audible provider) rather than calling Audible from the app (assessment).

### Audiobookshelf server as a metadata channel

- The ABS server ships these providers (source of truth:
  https://github.com/advplyr/audiobookshelf/tree/master/server/providers): `Audible`,
  `AudiobookCovers`, `Audnexus`, `FantLab`, `GoogleBooks`, `iTunes`, `MusicBrainz`, `OpenLibrary`,
  plus `CustomProviderAdapter`.
- **Custom metadata provider contract** (from
  https://github.com/advplyr/audiobookshelf/blob/master/server/providers/CustomProviderAdapter.js):
  the server calls `GET {providerUrl}/search?query=<title>[&author=][&isbn=]&mediaType=…`, sending an
  optional single opaque `Authorization` header configured per provider; the provider must respond
  `{"matches":[{ title, subtitle, author, narrator, publisher, publishedYear, description, cover,
  isbn, asin, genres[], tags[], series:[{series, sequence}], language, duration }]}`.
- **Implication**: this channel is a *server-side admin match flow* for filling library-item
  metadata — the payload has **no rating/review fields at all**, and results land in the ABS
  library, not in a client UI. Campfire could stand up a custom provider (e.g. a thin
  Hardcover-backed bridge) to improve library metadata server-side, but it cannot deliver
  per-user reviews, ratings, or upcoming-books UX in the app. Those need a direct client → provider
  integration.

## 10. How comparable open-source projects solved this (verified in source)

- **ShelfBridge** (ABS→Hardcover progress sync,
  https://github.com/rohit-purandare/ShelfBridge): calls
  `https://api.hardcover.app/v1/graphql` with a per-user `Authorization: Bearer <token>`; its error
  text tells users to "Generate a new token at https://hardcover.app/account/developer" —
  https://github.com/rohit-purandare/ShelfBridge/blob/main/src/hardcover-client.js. It supersedes
  the same author's `audiobookshelf-hardcover-sync`.
- **audiobookshelf-hardcover-sync** (https://github.com/drallgood/audiobookshelf-hardcover-sync):
  independent Go implementation of the same pattern — ABS API + Hardcover API with user tokens
  (repo description; per-file verification not performed).
- **Plappa** (iOS ABS/Jellyfin client, https://github.com/LeoKlaus/plappa): no third-party metadata
  provider found in the repo (a code search for audnexus/hardcover/goodreads returned nothing) — it
  displays what the media server provides. That is Campfire's status quo.
- **Readarr** — cautionary tale: the project was **retired** because "the project's metadata has
  become unusable, we no longer have the time to remake or repair it, and the community effort to
  transition to using Open Library as the source has stalled"; the community now points at
  third-party mirrors like `rreading-glasses` "at your own risk" —
  https://github.com/Readarr/Readarr (README announcement). Its metadata came from a proprietary
  Goodreads-derived proxy; when that broke, the product died. Lesson: don't build a core feature on
  scraped/unofficial upstreams.
- Ecosystem signal: the ABS↔Hardcover pairing is common enough that multiple independent sync tools
  exist (also `earmark-app/earmark`, "Bidirectional sync between Hardcover and Audiobookshelf",
  https://github.com/earmark-app/earmark) — user-pasted Hardcover PATs are an accepted UX in this
  community.

---

## Implications for Campfire

**Per use case:**

- **Reviews & ratings** → **Hardcover only** for real content (text reviews + rating distribution +
  averages), with mandatory "Hardcover rating" attribution on aggregates. Aggregate-only fallbacks
  where no Hardcover account exists: Open Library `ratings.json` (keyless, open) and/or Audnexus's
  Audible rating for audiobooks. Google Books averages are usable but drag in branding/link-out
  obligations.
- **Series-gap / upcoming detection** → **Hardcover** is the only practical source: verified float
  positions, `compilation` flag to drop box sets, `series.is_completed`, and placeholder entries for
  unreleased books with future release years. Wikidata (P179/P1545, CC0) is a possible free
  cross-check but coverage is uneven. Nothing else qualifies.
- **Supplemental metadata** → mostly already solved server-side by ABS's providers; for in-app
  enrichment prefer keyless open sources: Open Library (covers/descriptions/subjects; identify with
  a proper User-Agent; convert ISBN cover lookups to CoverID/OLID to dodge the 100/5-min limit) and
  Audnexus/AudiobookDB (ASIN-keyed audiobook data, author bios, chapters).

**Auth reality for a KMP client (Android/iOS/Desktop):**

| Provider | Shippable how |
|---|---|
| Hardcover | **Per-user pasted PAT** (use the PAT Link Builder to pre-select scopes); handle expiry (tokens have user-chosen expiration) and 401 → re-prompt; OAuth expected sometime in 2026 |
| Open Library, Audnexus, Wikidata/Inventaire | No auth — callable from the app directly; set an identifying User-Agent |
| Google Books | Embedded API key (restrictable, but quota shared across all installs) |
| AudiobookDB | Per-app or per-user API key (registration flow **UNVERIFIED**) |
| ISBNdb | Paid developer key — would have to be proxied to avoid embedding a billed secret; not worth it |

**Design consequences for the abstraction:**

1. Model capabilities per provider (`hasReviewText`, `hasAggregateRating`, `hasSeriesOrdering`,
   `hasUpcomingReleases`, `coversOnly`…), because no two providers overlap cleanly.
2. Treat Hardcover as an optional, user-linked account (like the existing ABS `UserScope`):
   store the PAT per user, respect its scopes/expiry, and surface the PAT-Link-Builder onboarding.
   Every rate limit is per-user (5k/day free), so client-side caching (Store5) plus honoring the
   `RateLimit`/`Retry-After` headers is enough — no shared backend needed.
3. Keep keyless providers (Open Library, Audnexus) as the anonymous default tier so the features
   degrade gracefully for users without a Hardcover account.
4. Cache Hardcover series/book data locally (their docs impose no caching prohibition; data is
   explicitly not claimed as proprietary), but keep user-owned content (others' reviews) display-only
   and attributed.
5. Do not build anything on Goodreads (retired + anti-scraping ToS), StoryGraph (no API),
   LibraryThing (disabled), or direct Audible endpoints (unofficial) — Readarr's retirement shows
   how that ends.
