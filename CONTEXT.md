# Campfire

An unofficial Kotlin Multiplatform client for Audiobookshelf. This glossary covers terms specific to the project that are not self-evident from the Audiobookshelf server's own vocabulary.

## Language

### Store screenshots

**Shot**:
One named store screenshot: a screen to reach, the device classes it applies to, and the locale and theme it is taken in.
_Avoid_: screenshot (when meaning the spec rather than the image), capture, screen

**Shot Spec**:
The single checked-in configuration listing every Shot and the fixtures needed to reach them.
_Avoid_: screenshot config, manifest

**Device Class**:
A store-defined screenshot category (phone, seven-inch tablet, ten-inch tablet) backed by one pinned emulator definition.
_Avoid_: form factor, device type, AVD (the AVD is what backs a Device Class, not the class itself)

**Fixture**:
The known server-side state a run starts from: the Sample Library, a fresh scan, and seeded listening progress.
_Avoid_: seed data, test data, demo data

**Sample Library**:
The generated, fake audiobook set used for screenshots.
_Avoid_: fake library, test library, screenshot library

**Store Metadata**:
The `fastlane/` tree that is the source of truth for store listings, including screenshots.
_Avoid_: fastlane (when meaning the directory rather than the tool), assets, art
