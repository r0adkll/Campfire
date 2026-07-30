# Fastlane metadata

This directory holds the [Fastlane supply](https://docs.fastlane.tools/actions/supply/) metadata
structure consumed by F-Droid and IzzyOnDroid to render Campfire's store listing (and usable with
`fastlane supply` for Google Play).

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
