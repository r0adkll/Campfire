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

When tagging a release, add `changelogs/<versionCode>.txt` summarizing that release's user-facing
changes from `CHANGELOG.md` (keep it under 500 characters). The versionCode is derived from the
tag by `gradle/build-logic` (`Versioning.kt`, `MMmmppRR` — e.g. `1.0.0-rc3` → `1000003`,
`1.2.3` → `1020399`). F-Droid and IzzyOnDroid read these files from the repository's default
branch, so the changelog can be committed before or after the tag is pushed.

Screenshots live canonically in `.github/art/screens/`; the copies here are curated for store
listings (portrait phone shots only — the AI theme builder shot is excluded because that feature
is not part of the FOSS flavor).
