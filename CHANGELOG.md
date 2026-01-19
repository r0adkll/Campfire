# Change Log

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- OIDC Authentication
- Offline downloads accessible from side drawer
- Denser grid option for the Library screen

### Changed

- Switched underlying authentication to use bearer auth for improved security.
- Streaming / Image requests now use HTTP headers for authorization.
- Moved palette picker on item detail from cover image to top bar
- Improved navigation shared element animations

### Deprecated

### Removed

### Fixed

- Analytics consent screen no longer re-appears after accepting it
- Predictive back navigation causing screens to infinitely load
- Analytics consent screen re-appearing after accepting it
- Search overlay popping up when adding bookmarks
- Bug where home feed would be empty when using multiple accounts/libraries

### Other Notes & Contributions

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
