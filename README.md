<p align="center">
<img src=".github/art/GithubBanner.png" width="100%" />
</p>

# Campfire

[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/D1D5KEED)

**Campfire** is an unofficial app for [Audiobookshelf](https://www.audiobookshelf.org/) built in Kotlin/Compose Multiplatform for a more native experience than the official app.

## Install

<a href="https://play.google.com/store/apps/details?id=app.campfire.android"><img width=200 src=".github/art/badge_playstore.png" /></a>
<a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22%3A%22app.campfire.android%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2Fr0adkll%2FCampfire%22%2C%22author%22%3A%22r0adkll%22%2C%22name%22%3A%22Campfire%22%2C%22preferredApkIndex%22%3A1%2C%22additionalSettings%22%3A%22%7B%5C%22includePrereleases%5C%22%3Afalse%2C%5C%22fallbackToOlderReleases%5C%22%3Atrue%2C%5C%22filterReleaseTitlesByRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22filterReleaseNotesByRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22verifyLatestTag%5C%22%3Afalse%2C%5C%22sortMethodChoice%5C%22%3A%5C%22date%5C%22%2C%5C%22useLatestAssetDateAsReleaseDate%5C%22%3Afalse%2C%5C%22releaseTitleAsVersion%5C%22%3Afalse%2C%5C%22trackOnly%5C%22%3Afalse%2C%5C%22versionExtractionRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22matchGroupToUse%5C%22%3A%5C%22%5C%22%2C%5C%22versionDetection%5C%22%3Atrue%2C%5C%22releaseDateAsVersion%5C%22%3Afalse%2C%5C%22useVersionCodeAsOSVersion%5C%22%3Afalse%2C%5C%22apkFilterRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22invertAPKFilter%5C%22%3Afalse%2C%5C%22autoApkFilterByArch%5C%22%3Atrue%2C%5C%22minimumUpdateAgeDays%5C%22%3A%5C%22%5C%22%2C%5C%22appName%5C%22%3A%5C%22%5C%22%2C%5C%22appAuthor%5C%22%3A%5C%22%5C%22%2C%5C%22shizukuPretendToBeGooglePlay%5C%22%3Afalse%2C%5C%22allowInsecure%5C%22%3Afalse%2C%5C%22allowedSigningCertHashes%5C%22%3A%5C%22%5C%22%2C%5C%22exemptFromBackgroundUpdates%5C%22%3Afalse%2C%5C%22skipUpdateNotifications%5C%22%3Afalse%2C%5C%22about%5C%22%3A%5C%22%5C%22%2C%5C%22refreshBeforeDownload%5C%22%3Afalse%2C%5C%22includeZips%5C%22%3Afalse%2C%5C%22zippedApkFilterRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22includeTarballs%5C%22%3Afalse%2C%5C%22tarballedApkFilterRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22url%5C%22%3A%5C%22https%3A%2F%2Fapi.github.com%2Frepos%2Fr0adkll%2FCampfire%2Freleases%2Fassets%2F543577244%5C%22%2C%5C%22enableCertificatePinning%5C%22%3Afalse%7D%22%2C%22overrideSource%22%3A%22GitHub%22%7D"><img width=200 src=".github/art/badge_obtainium.png" /></a>
<a href="https://github.com/r0adkll/Campfire/releases/latest"><img width=200 src=".github/art/
badge_github.png" /></a>
<a href="https://appdistribution.firebase.dev/i/14b078b4670cc57e"><img width=200 src=".github/art/badge_firebase_alpha.png" /></a>
<a href="https://appdistribution.firebase.dev/i/6021e2e24ae35f4c"><img width=200 src=".github/art/badge_firebase_beta.png" /></a>

## Setup

- [OIDC Setup](docs/oidc_setup.md) — configure the `campfireaudiobooks://oauth` redirect URI in Audiobookshelf


## Screenshots
| Home                              | Detail                              | Player                                  |
|-----------------------------------|-------------------------------------|-----------------------------------------|
| ![](.github/art/screens/Home.png) | ![](.github/art/screens/Detail.png) | ![](.github/art/screens/PlayerView.png) |

| Library                              | Search                              | Stats                              |
|--------------------------------------|-------------------------------------|------------------------------------|
| ![](.github/art/screens/Library.png) | ![](.github/art/screens/Search.png) | ![](.github/art/screens/Stats.png) |

| Foldable - Home                           | Foldable - Detail                           |
|-------------------------------------------|---------------------------------------------|
| ![](.github/art/screens/FoldableHome.png) | ![](.github/art/screens/FoldableDetail.png) |

## Architecture
Head over to [Architecture](docs/architecture/README.md) for more detailed information on the architecture of this project.

## Contributing

Please follow the guidelines set forth in the [CONTRIBUTING](CONTRIBUTING.md) document.

## License

GNU General Public License v3.0

See [LICENSE](LICENSE) to see the full text.
