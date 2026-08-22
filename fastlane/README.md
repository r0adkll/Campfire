# Fastlane metadata

This directory holds the [Fastlane supply](https://docs.fastlane.tools/actions/supply/) metadata
structure consumed by F-Droid and IzzyOnDroid to render Campfire's store listing, and the
`Fastfile` lanes that publish to Google Play (App Store lanes will join them later).

## Layout

```
metadata/android/en-US/
├── title.txt                  # App name
├── short_description.txt      # Max 80 characters
├── full_description.txt       # Basic HTML allowed (F-Droid renders <p>/<ul>/<li>/<a>/<b>)
├── changelogs/
│   └── <versionCode>.txt      # Per-release changelog, max 500 characters
└── images/
    ├── icon.png               # 512x512
    ├── featureGraphic.png     # 1024x500
    └── phoneScreenshots/      # Numbered for display order
```

## Cutting a release

Run `scripts/prepare-release <version>` (e.g. `scripts/prepare-release 1.0.0-rc4`). It bumps
`campfire.version` in `gradle.properties`, rolls the `[Unreleased]` section of `CHANGELOG.md`
into a `[<version>]` section, and writes `changelogs/<versionCode>.txt` here from those entries —
review and trim that file to a user-facing summary under 500 characters (the script warns when
it's over). The versionCode is derived from the version the same way `gradle/build-logic`
(`Versioning.kt`) derives it from the tag (`MMmmppRR` — e.g. `1.0.0-rc3` → `1000003`,
`1.2.3` → `1020399`). F-Droid and IzzyOnDroid read these files from the repository's default
branch, so the changelog can be committed before or after the tag is pushed.

Screenshots live canonically in `.github/art/screens/`; the copies here are curated for store
listings (portrait phone shots only — the AI theme builder shot is excluded because that feature
is not part of the FOSS flavor).

## Google Play publishing

Building stays in Gradle; the lanes in `Fastfile` only move built artifacts and this listing
into Google Play. CI drives them, so fastlane is not needed locally (`bundle install` from the
repo root if you want to run a lane by hand).

| Lane | When | What |
|---|---|---|
| `android beta` | pre-release (`-rcN`) GitHub release | AAB to the **open testing** track |
| `android production` | final GitHub release | AAB to **production** as a 20% staged rollout |
| `android rollout` | `Play Rollout` workflow (manual) | widen/complete the staged rollout |
| `android metadata` | `Play Metadata` workflow (listing changes on `main`, or manual) | descriptions, icon, feature graphic, screenshots |

Release notes for each upload are read from `changelogs/<versionCode>.txt` — the same file
IzzyOnDroid and F-Droid use, so `scripts/release` only writes it once.

Authentication is a Play Console service account passed as the `PLAY_SERVICE_ACCOUNT_JSON`
repository secret (`SUPPLY_JSON_KEY_DATA` for fastlane). Every Play job is skipped while that
secret is absent. Note that the Publishing API refuses an app that has never had a bundle
uploaded through the Console, so the very first AAB of a new listing is uploaded by hand.
The privacy policy URL for the listing is https://thescavengers.software/campfire/privacy.
