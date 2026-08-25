# Change Log

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

### Changed

### Deprecated

### Removed

### Fixed

- App widgets breaking due to large image size
- Cast devices not appearing reliably, including the cast button vanishing after rotating the screen
- Selecting a cast device could hang audio playback until the app was restarted — connection failures now show in the device picker and playback automatically falls back to this phone

### Other Notes & Contributions

- The cast device list now offers to request local network access on Android 16+ when it may be needed to find devices

## [1.0.5]

### Other Notes & Contributions

- FOSS (F-Droid) builds are now byte-for-byte reproducible

## [1.0.4]

### Fixed

- What's New screen could occasionally ship without its changelog content

### Other Notes & Contributions

- Made the FOSS (F-Droid) build reproducible while keeping the baseline profile for startup performance

## [1.0.3]

### Fixed

- Mini player cover art no longer stays blank while a book is playing

### Other Notes & Contributions

- Releases now publish to Google Play automatically: release candidates to open testing, final versions as a staged production rollout

- `scripts/release` is now a guided interactive flow that keeps Gradle output in a log file and helps resolve blockers such as an over-limit store changelog

## [1.0.2]

### Added

- Tap the speed value in the playback speed sheet to type an exact playback speed

### Changed

- Cover art is now requested at the size it is displayed, so full-screen player and detail artwork is sharp instead of upscaled

### Fixed

- App freezing on the search empty state when system animations are disabled (e.g. the "Remove animations" accessibility setting)
- Series with decimal sequence numbers (1.1, 1.2, …) now sort in the correct order instead of jumping to the end
- Covers updated on the server now refresh in the app instead of showing the old image until the cache expires
- Download and file sizes showing two decimal places (e.g. 1.00 MB) instead of one

### Other Notes & Contributions

- Added `scripts/release` to cut releases in one step; the Release workflow now bumps `campfire.version` automatically after a release
- Alpha build release notes now describe the PR that produced the build instead of the cumulative Unreleased changelog
- Baseline profiles are generated once per release (on emulator.wtf in CI, or locally via `scripts/release`) instead of once per flavor on a hosted-runner emulator

## [1.0.1]

### Fixed

- Crash when the server is unreachable or an item was deleted while opening Settings or resuming playback
- Crash when resuming downloads on app launch on some Android 12+ devices
- Crash when opening Android Auto settings on devices without Android Auto installed
- Playback resumption from Bluetooth / media buttons killing the app when the server is offline
- Playback speeds between x.01 and x.09 displaying incorrectly (e.g. 1.03x showing as 1.3x)

### Other Notes & Contributions

- Fixed the Google Play release bundle build failing on a lint check

## [1.0.0]

### Added

- Google Play builds can now update the app in-place when a new version is available on the Play Store

### Changed

- The bluetooth next/previous settings default is now to fast-forward / rewind

### Fixed

- Books belonging to multiple series now show all of their series on the book detail screen
- App freezing completely when connecting to Android Auto
- Logging in with an account that has no library access now explains the problem instead of incorrectly reporting bad credentials

### Other Notes & Contributions

- The on-device AI theme builder is now an optional component, laying the groundwork for fully FOSS builds for F-Droid
- Added a new FOSS build variant with no Google or analytics components, in preparation for F-Droid and IzzyOnDroid distribution
- Google Cast support is now an optional component excluded from the FOSS build variant
- Release version numbers are now derived from the release tag so builds are reproducible
- Refreshed the in-app open source license list, which now accurately reflects each build variant's dependencies

## [1.0.0-rc3]

### Fixed

- Crash when the server sends large or malformed realtime sync updates, such as progress updates for accounts with a long listening history

## [1.0.0-rc2]

### Fixed

- Crash on app launch when opening the app while logged out or when your account needs to be re-authenticated

## [1.0.0-rc1]

### Added

- List/grid display toggle for the Series, Collections, and Playlists screens
- Optional overall book progress indicator with time remaining in the player, toggleable in Playback settings
- Setting to disable the wavy playback seek bar animation
- Quick-insert bar above the keyboard when typing the server address at login, with one-tap snippets for https://, http://, local network prefixes, and the default Audiobookshelf port
- Server addresses no longer require typing http(s):// — the login screen detects the right scheme automatically and fills it in
- eBook-only library items now show a format badge on their covers and their play, download, and queue actions are disabled since reading eBooks isn't supported yet
- Android Auto tabs now adapt to the active library: podcast libraries get a Shows tab for browsing every podcast and its episodes, hide the book-only Series, Authors, and Collections tabs, and the tab list refreshes live when switching libraries or changing Android Auto settings

### Changed

- Collection, series, and playlist cards now show an item-count badge in the corner of the cover
- Server addresses are now redacted from diagnostic logs and crash reports to better protect your privacy
- The tent picker on the login screen now offers the app's default themes, previews your choice live, and applies it as your app theme after signing in

### Deprecated

### Removed

- The per-server tent color: the tent picker in Account settings is gone and campsite icons now share one look — theming is handled entirely by the theme picker

### Fixed

- Offline playback of downloaded audiobooks failing when the server is unreachable
- Crash when the server sends a real-time library update containing unusual line-break characters (seen with some podcast descriptions)
- Crash on launch when more than one account on the device had listening progress for the same book
- Server addresses that resolve to a local IP through custom DNS being rejected on Android 16+ (the local network permission is now requested for them)
- Campfire logo flame flickering during the Welcome to Login screen transition
- Real-time sync socket not connecting after logging in until the app was restarted
- Podcasts in Android Auto can now be browsed into their episode list, and episode entries show the episode's title, artwork, and duration and play the correct episode instead of failing to start
- Android Auto category tabs hanging indefinitely instead of showing an empty list when the library has no playlists, series, authors, or downloads
- Android Auto browse and search lists now respect the head unit's requested page size instead of returning every item at once, fixing truncated lists in large libraries

### Other Notes & Contributions

## [0.13.1-beta]

### Added

- Auto-rewind configurable durations after paused

### Fixed

- Missing runtime permission on Android 17 when connecting to local network addresses

## [0.13.0-beta]

### Added

- **Podcasts**
  - Add new podcasts
  - Find and add new episodes
  - Server download queue
  - Latest episodes screen
- **Socket Connection**
  - Now supports the socket.io connection to the server
  - Near-immediate syncing of item, series, playlists, collection, author changes from the server or other devices
  - Improved progress syncing
  - Live podcast episode download queue / notifications
- New AI theme generator using local-only Gemini Nano via Halogen

### Changed

- Change the theme mode or open the theme picker directly from the Drawer

### Fixed

- Phone Landscape now has a friendlier UI/UX

## [0.12.0-beta]

### Added

- Playback History
- Multi-device progress synchronization
- Setting to disable marquee scrolling on library item cards
- Android Auto settings pane: toggle, reorder, and choose list/grid layout per top-level category
- 37 new theme icons (camping/nature/travel) selectable when building a custom theme

### Changed

- Already listened to chapters on items in-progress are collapsed by default
- Overhauled playback homescreen widget UI
- Theme color extraction from cover art now waits for a brief impression before dispatching, avoiding wasted work when scrolling past items
- Time remaining on collapsed bar + item detail now reflect the current playback speed of a session
- Improved home feed scrolling with mixed scrolling orientations

### Fixed

- Cast / Output device list not always showing available / current device(s)
- Fixed media progress not syncing during playback
- Fixed playback sessions dying due to token expiration
- Device information reported as 'Unknown' for sessions on the server
- Cover art not always rendering in Android Auto

### Other Notes & Contributions

## [0.11.0-beta]

### Added

- What's New / Changelog screen and widget
- Playback Queue
- Playlists
- Setting to switch next/prev actions from remotes (i.e. Bluetooth) to fast forward / rewind
- Support the [playback resumption][media3-playback-resumption] API

### Changed

- Revamped the sleep timer bottom sheet UI in the player view
- Warnings when an item has misaligned chapter information that needs to be corrected on the server
- Rewind action will jump to previous chapter with the rewind duration from the end instead of just to the start of the item
- Default playback speed options changed to 1x, 1.1x, 1.25x, 1.5x, 2x. Slider range remains 0.5x to 2x.

### Fixed

- Bottom navigation bar now transitions smoothly between shared screen transitions
- Series on the item detail page not appearing until loading them in the series screen
- App crashing on system reboot due to offline download resumption
- Mark Finished / Discard Progress would pause playback of current item
- Playback issues when chapter information is mis-aligned with the audio tracks
- Bluetooth metadata not showing the title of the book

## [0.10.0-beta] - 2026-02-01

### Added

- Paging Support! Library items, series, and authors are all now paginated for large library support
- Item progress now displays in contexts other than the Home feed

### Changed

- "Play/Continue listening" button on item details is now disabled if it is the current play session

### Fixed

- Crash when opening series detail page
- Death loop when using multiple accounts and all refresh tokens expire
- Collection book ordering not being consistent with web/server
- Fast Forward / Rewind not working on Android Auto
- Collections not being editable if you were a "root" user
- Playback view now maintains expansion state on configuration changes

## [0.9.0-beta] - 2026-01-19

### Added

- OIDC Authentication
- Offline downloads accessible from side drawer
- Denser grid option for the Library screen

### Changed

- Switched underlying authentication to use bearer auth for improved security.
- Streaming / Image requests now use HTTP headers for authorization.
- Moved palette picker on item detail from cover image to top bar
- Improved navigation shared element animations

### Fixed

- Analytics consent screen no longer re-appears after accepting it
- Predictive back navigation causing screens to infinitely load
- Analytics consent screen re-appearing after accepting it
- Search overlay popping up when adding bookmarks
- Bug where home feed would be empty when using multiple accounts/libraries
- Library filter(s) now persist between screens and configuration changes

## [0.8.0-beta] - 2025-12-16

### Added

- New dynamic content theming based on an item's thumbnail.
- Ability to change the seed color for per-item theming
- Settings to enable/disable dynamic content based theming

### Changed

- Material 3 Expressive UI overhaul
- Rewrote item detail screen for performance
- Item detail screen UI polishing.
- Moved 'tent' choice/theming to 'Appearance' settings tab
- Moved 'Collections' to the left-drawer from bottom nav bar
- Editing collections now only shows for server admins

### Removed

- Top bar library switcher. This is now located only in the left-hand drawer.

### Fixed

- Incorrect stat % trend rendering when negative from the previous week
- Series not always appearing on item details.
- Inconsistent series book ordering
- Series disk cache getting deleted when leaving series screen
- Crash due to null 'duration' value in MediaProgress
- Crash due to float 'time' in Bookmark responses
- Crash due to missing or misaligned chapters during playback / ui
- Crash due to no user stat information
- Home feed not syncing / displaying correctly

## [0.7.2-beta] - 2025-11-06

### Fixed

- Fixed metadata parsing bug causing some items and collections to not load in the UI

## [0.7.1-beta] - 2025-11-06

### Fixed

- Gracefully handle Cast/PlayService integrations on devices that don't have Google/Play Services

## [0.7.0-beta] - 2025-11-06

### Added

- Support for Google Cast
- Manage offline downloads in settings screen

### Changed

- Refactored how the Home screen observes its data
- Release notes display in in-app update card
- In-app update now shows progress
- "Mark as (Not) Finished" is always visible

### Fixed

- Opening/Closing foldables no properly manages detail screens.
- Screen jank when app widget is present and playing a book
- How the homescreen loads its data leading to blank / flickering items

## [0.6.2-beta] - 2025-10-31

### Fixed

- Fixed model parsing in Home feed and collection views
- Fixed crashing when pressing system back button

## [0.6.1-beta] - 2025-10-30

### Fixed

- Fixed issue with how data was being parsed on the home feed

## [0.6.0-beta] - 2025-10-30

### Added

- Usage analytics via MixPanel (Android Only)
- Reporting consent screen to opt-out of developer and usage reporting
- More debugging to help diagnose infinite loading screens

### Changed

- Disabled Firebase in debug builds
- Capitalize "Chapter" in all "End of Chapter" text

### Fixed

- Improved UI performance during playback
- Playback speed now displays down to the hundredth place
- Improved sleep timer display of long chapters
- Chapter time in the playback UI scales with playback speed
- Series books are now more consistently sorted in the correct order
- Issue where Play/Pause would become disabled when session is loaded
- In-app updating via Firebase App Distribution for beta and alpha builds

### Other Notes & Contributions

- Reached database stability. From this release onwards re-installs won't be required.

## [0.5.0-alpha] - 2025-10-12

### Added

- Library item filtering
- Save playback speed across listening sessions
- Toggle between time in book and chapter length in Now Playing chapter list
- Item detail author/narrator now clickable
- Shared animation transitions throughout entire app
- Current library picker added to drawer screen

### Changed

- Item Detail UI Adjusted to be more streamlined for downloads
- Improved empty and default states in Author UI
- Author placeholder image is now gender neutral

### Fixed

- Navigating from search for Narrators, Genres, and Tags now works
- Logging out & Changing accounts
- Status bar now respects app theme

## [0.4.0-alpha] - 2025-07-07

### Added

- Android Auto Support
- Toggle on item detail to switch between time in book, and chapter length

### Changed

- When playback speed is not 1x, display the actual speed in the actions bar
- Dynamic timer icon when timer is enabled in playback UI
- Time remaining in the playback UI now adjusts based on playback speed
- Improved the homescreen widget appearance and actions
- Widget can now expand vertically showing the list of chapters for the current item.

### Fixed

- HTML is now rendered properly on the item detail page description
- Fixed sleep timer fading-to-pause when set as "End of Chapter"
- Fixed how PlayMethod not reporting offline play correctly

## [0.3.0-alpha] - 2025-07-02

### Added

- Library switcher UI for changing your selected library.
- Offline status indicators on all surfaces that show library items.
- Android home screen widget to control playback

### Fixed

- Fix issue with non-ssl traffic for self-hosted servers not behind https

## [0.2.1-alpha] - 2025-06-22

### Added

- Download confirmation dialog
- Download settings panel
- Android permissions check for download notification
-
### Fixed

- Fixed [#237](https://github.com/r0adkll/Campfire/issues/237) whens syncing media progress after login

## [0.2.0-alpha] - 2025-06-21

### Added

- Ability to add books to collections, or create new collections
- Ability to remove books from collections, and delete collections
- Offline download support for Android

### Fixed

- Loading indicators not showing on series/collections screens for initial load
- Crash on the 'Statistics' page when previous week didn't have any data

## [0.1.0-alpha] - 2025-06-15

### Added

- Initial Alpha Release.

[1.0.5]: https://github.com/r0adkll/Campfire/compare/1.0.4...1.0.5
[1.0.4]: https://github.com/r0adkll/Campfire/compare/1.0.3...1.0.4
[1.0.3]: https://github.com/r0adkll/Campfire/compare/1.0.2...1.0.3
[1.0.2]: https://github.com/r0adkll/Campfire/compare/1.0.1...1.0.2
[1.0.1]: https://github.com/r0adkll/Campfire/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/r0adkll/Campfire/compare/v1.0.0-rc3...v1.0.0
[1.0.0-rc3]: https://github.com/r0adkll/Campfire/compare/v1.0.0-rc2...v1.0.0-rc3
[1.0.0-rc2]: https://github.com/r0adkll/Campfire/compare/v1.0.0-rc1...v1.0.0-rc2
[1.0.0-rc1]: https://github.com/r0adkll/Campfire/compare/v0.13.1-beta...v1.0.0-rc1
[0.13.1-beta]: https://github.com/r0adkll/Campfire/compare/v0.12.0-beta...v0.13.1-beta
[0.13.0-beta]: https://github.com/r0adkll/Campfire/compare/v0.12.0-beta...v0.13.0-beta
[0.12.0-beta]: https://github.com/r0adkll/Campfire/compare/v0.11.0-beta...v0.12.0-beta
[0.11.0-beta]: https://github.com/r0adkll/Campfire/compare/v0.10.0-beta...v0.11.0-beta
[0.10.0-beta]: https://github.com/r0adkll/Campfire/compare/v0.9.0-beta...v0.10.0-beta
[0.9.0-beta]: https://github.com/r0adkll/Campfire/compare/v0.8.0-beta...v0.9.0-beta
[0.8.0-beta]: https://github.com/r0adkll/Campfire/compare/v0.7.2-beta...v0.8.0-beta
[0.7.2-beta]: https://github.com/r0adkll/Campfire/compare/v0.7.1-beta...v0.7.2-beta
[0.7.1-beta]: https://github.com/r0adkll/Campfire/compare/v0.7.0-beta...v0.7.1-beta
[0.7.0-beta]: https://github.com/r0adkll/Campfire/compare/v0.6.2-beta...v0.7.0-beta
[0.6.2-beta]: https://github.com/r0adkll/Campfire/compare/v0.6.1-beta...v0.6.2-beta
[0.6.1-beta]: https://github.com/r0adkll/Campfire/compare/v0.6.0-alpha...v0.6.1-beta
[0.6.0-beta]: https://github.com/r0adkll/Campfire/compare/v0.5.0-alpha...v0.6.0-beta
[0.5.0-alpha]: https://github.com/r0adkll/Campfire/compare/v0.4.0-alpha...v0.5.0-alpha
[0.4.0-alpha]: https://github.com/r0adkll/Campfire/compare/v0.3.0-alpha...v0.4.0-alpha
[0.3.0-alpha]: https://github.com/r0adkll/Campfire/compare/v0.2.1-alpha...v0.3.0-alpha
[0.2.1-alpha]: https://github.com/r0adkll/Campfire/compare/v0.1.0-alpha...v0.2.1-alpha
[0.2.0-alpha]: https://github.com/r0adkll/Campfire/compare/v0.1.0-alpha...v0.2.0-alpha
[0.1.0-alpha]: https://github.com/r0adkll/Campfire/releases/tag/v0.1.0

[media3-playback-resumption]: https://developer.android.com/media/media3/session/background-playback?utm_source=android-studio-app&utm_medium=app#resumption
