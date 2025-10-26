# Change Log

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Usage analytics via MixPanel (Android Only)
- Reporting consent screen to opt-out of developer and usage reporting

### Changed

- Disabled Firebase in debug builds

### Deprecated

### Removed

### Fixed

- Improved UI performance during playback
- Improved sleep timer display of long chapters
- Chapter time in the playback UI scales with playback speed

### Other Notes & Contributions

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

[0.5.0-alpha]: https://github.com/r0adkll/Campfire/compare/v0.4.0-alpha...v0.5.0-alpha
[0.4.0-alpha]: https://github.com/r0adkll/Campfire/compare/v0.3.0-alpha...v0.4.0-alpha
[0.3.0-alpha]: https://github.com/r0adkll/Campfire/compare/v0.2.1-alpha...v0.3.0-alpha
[0.2.1-alpha]: https://github.com/r0adkll/Campfire/compare/v0.1.0-alpha...v0.2.1-alpha
[0.2.0-alpha]: https://github.com/r0adkll/Campfire/compare/v0.1.0-alpha...v0.2.0-alpha
[0.1.0-alpha]: https://github.com/r0adkll/Campfire/releases/tag/v0.1.0
