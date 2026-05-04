<p align="center">
<img src=".github/art/GithubBanner.png" width="100%" />
</p>

# Campfire

[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/D1D5KEED)

**Campfire** is an unofficial app for [Audiobookshelf](https://www.audiobookshelf.org/) built in Kotlin/Compose Multiplatform for a more native experience than the official app.

> [!IMPORTANT]
> This is a **WIP** project and no timeline will be given at this point in time

## Install
If you would like to install **Campfire** and test it out head over to the [releases page](https://github.com/r0adkll/Campfire/releases) and download the latest APK on your device.

OR stay up to date with the latest release using

| Alpha Builds | Beta Builds |
| --- | --- |
| Unstable builds generated on every change | Stable builds generated from release page |
| [<img width=300 src=".github/art/FirebaseAppDistribution.svg"/>](https://appdistribution.firebase.dev/i/6021e2e24ae35f4c) | [<img width=300 src=".github/art/FirebaseAppDistribution.svg"/>](https://appdistribution.firebase.dev/i/14b078b4670cc57e) |

## Tech Stack

* [Kotlin Multiplatform][kotlin-multiplatform]
* [Jetbrains Compose Multiplatform][compose-multiplatform]
* Presentation Architecture: [Slack's Circuit][slack-circuit]
* Networking: [Ktor Client][ktor]
* Storage
  * [SQLDelight][sql-delight]
  * [Store5][store]
* Dependency Injection
  * [kotlin-inject][kinject]
  * [kimchi][kimchi]
* Analytics
  * [MixPanel][mix-panel]
  * [Firebase][firebase]

[kotlin-multiplatform]: https://kotlinlang.org/docs/multiplatform.html
[compose-multiplatform]: https://www.jetbrains.com/lp/compose-multiplatform/
[slack-circuit]: https://slackhq.github.io/circuit/
[ktor]: https://ktor.io/docs/welcome.html
[sql-delight]: https://cashapp.github.io/sqldelight/2.0.0/multiplatform_sqlite/
[store]: https://github.com/MobileNativeFoundation/Store
[kinject]: https://github.com/evant/kotlin-inject
[kimchi]: https://github.com/r0adkll/kimchi
[mix-panel]: https://docs.mixpanel.com/docs/tracking-methods/sdks/android
[firebase]: https://firebase.google.com/

## Architecture
Head over to [Architecture](docs/architecture/README.md) for more detailed information on the architecture of this project.

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

## Contributing

Please follow the guidelines set forth in the [CONTRIBUTING](CONTRIBUTING.md) document.

## License

GNU General Public License v3.0

See [LICENSE](LICENSE) to see the full text.
