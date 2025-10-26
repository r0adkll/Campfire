<p align="center">
<img src=".github/art/GithubBanner.png" width="100%" />
</p>

# Campfire

**Campfire** is an unofficial app for [Audiobookshelf](https://www.audiobookshelf.org/) built in Kotlin/Compose Multiplatform for a more native experience than the official app.

# Architecture

## Module Types

### Standalone

```
module-name/
└── src/
    └── …
```

These are modules that are very self-contained and serve a singular purpose / concern.

### Split

```
module-name/
├── api/
│   └── src/
│       └── …
├── impl/
│   └── src/
│       └── …
└── ui/
    └── src/
        └── …
```

These are a group of modules for building features that provide function to other features/modules and ui/screens.
* `:api` - A lightweight module that can only depend on `:core` or other infra modules without other dependencies.
* `:impl` - The implementation module that provides the implementations and bindings for `:api`. This is only implemented by the `:app` module(s)
* `:ui` - This module consumes `:api` and any other feature `:api` modules to provide Circuit screen implementations _(more on this later)_. This is only implemented by the `:app` module.

## This is a **WIP** project and no timeline will be given at this point in time

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

[kotlin-multiplatform]: https://kotlinlang.org/docs/multiplatform.html
[compose-multiplatform]: https://www.jetbrains.com/lp/compose-multiplatform/
[slack-circuit]: https://slackhq.github.io/circuit/
[ktor]: https://ktor.io/docs/welcome.html
[sql-delight]: https://cashapp.github.io/sqldelight/2.0.0/multiplatform_sqlite/
[store]: https://github.com/MobileNativeFoundation/Store
[kinject]: https://github.com/evant/kotlin-inject
[kimchi]: https://github.com/r0adkll/kimchi

## Contributing

Please follow the guidelines set forth in the [CONTRIBUTING](CONTRIBUTING.md) document.

<a href='https://ko-fi.com/D1D5KEED' target='_blank'><img height='36' style='border:0px;height:36px;' src='https://az743702.vo.msecnd.net/cdn/kofi1.png?v=0' border='0' alt='Buy Me a Coffee at ko-fi.com' /></a>

## License

GNU General Public License v3.0

See [LICENSE](LICENSE) to see the full text.
