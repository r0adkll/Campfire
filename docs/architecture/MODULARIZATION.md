# Modularization

This page describes the types of modules / groups that are used to build this application for all
module types that this architecture encompasses. Think of these as the module building blocks for this
architecture.

### Standalone

```
module-name/
└── src/
    └── …
```

These are modules that are very self-contained and serve a singular purpose / concern. Examples of these are:

- `:core` - The central common module that provides the basic types, and common utilities to ALL modules.
- `:ui:appbar` - A module that provides the common top-level app bar + logic for several features/screens in the app.
- `:infra:debug` - A module that provides in-app debug UI and functionality for the app.
- `:infra:shake` - A module that provides KMP shake detection.

### Grouped

```
module-name/
├── api/
│   └── src/
│       └── …
├── impl/
│   └── src/
│       └── …
│
---- OPTIONAL ----
│
├── ui/
│   └── src/
│       └── …
├── public-ui/
│   └── src/
│       └── …
└── test/
    └── src/
        └── …
```

These are a group of modules for building features that provide function to other features/modules and ui/screens.
* **`:api`** - A lightweight module that can only depend on `:core` or other infra modules without other dependencies.
* **`:impl`** - The implementation module that provides the implementations and bindings for `:api`. This is only implemented by the `:app` module(s)
* **`:ui`** _[optional]_ - This module consumes `:api` and any other feature `:api` modules to provide Circuit screen implementations _(more on this later)_. This is only implemented by the `:app` module.
* **`:public-ui`** _[optional]_ - This should **_ONLY_** be used to share common ui components to other features that require logic specific to that module that wouldn't make it a good fit for the common ui/widgets module.
* **`:test`** _[optional]_ - This modules contains test utility code for the `:api` module in this group. This only exposes fakes, model fixtures, etc. This should NEVER expose any other module from this group than `:api`

## Module Types

### Common Modules
These are very few and lightweight modules meant to be shared with ALL other modules

- `:core` - This contains central elements such as domain models, DI infrastructure (scopes, qualifiers, etc), logging, application initializers, and other common utilities and extensions.
- `:common:compose` - This contains common Compose elements such as icons, shared composable widgets/layouts, application theme, and other shared Compose utilities.
- `:common:screens` - This contains the central elements for constructing Circuit screen keys / data classes. This use to host ALL keys, but these are being modularized to their feature `:api` modules.

### Feature Modules
This group of modules contains the bulk of all the application feature code. From authentication and sign-in to library item list and detail pages. MOST user facing code subsides in these modules. e.g.

- `:features:auth` - This contains the welcome, sign-in, and analytic consent collection screens.
- `:feautres:libraries` - This contains the library item detail and list screens, as well as the apis for interacting with libraries and their items.
- `:features:sessions` - This contains the playback session apis and playback bar UIs.

_Check out the code in the other `:features` modules for tons of more examples._

### Data Modules
This group of modules contains a set of infra tool modules for driving data through the app. This includes:

- `:data:account` - Set of modules for managing user accounts, user sessions, authentication tokens, and UI elements for switching and picking accounts.
- `:data:analytics` - Set of modules for managing usage analytics.
- `:data:crashreporting` - Set of modules for crash reporting and other developer related metrics through Firebase.
- `:data:db` - Central database and its related modules and functions.
- `:data:mapping` - Module for mapping between DB types and domain types.
- `:data:network` - Central networking layer for communicating with the audiobookshelf APIs.

### Infra Modules
This group of modules contains the set of core infrastructure modules that provide utility and function to non-feature
specific functions of the app. This includes:

- `:infra:audioplayer` - Provides a central interface for interacting with the audio player for all platforms. (`ExoPlayer` for Android, `AVPlayer` for iOS, and `VLC` for Desktop).
- `:infra:shake` - Provides "Shake" detection for all mobile platforms.
- `:infra:updates` - Provides a central interface for in-app updates.
- `:infra:debug` - Provides in-app debug UI and functionality for the app.

### UI modules
Lastly this contains a set of modules for providing independent re-usable UI elements for the application. Such as:

- `:ui:appbar` - Provides the top-level common `CampfireAppBar` implementation that includes its own presenter/logic.
- `:ui:attribution` - Provides the in-app UI solution to `aboutlibraries` library for providing attributions per open source legal policy.
- `:ui:drawer` - Provides the navigation drawer implementation.
- `:ui:widgets` - Provides the homescreen widget implementations for the app.

## Feature Graph Structure

```mermaid
%%{
  init: {
    'theme': 'neutral'
  }
}%%

graph LR
  :features["features"]
  :app["app"]
  :thirdparty["thirdparty"]
  :scripts["scripts"]
  :app["app"]
  :features["features"]
  :scripts["scripts"]
  :thirdparty["thirdparty"]
  subgraph :features
    :features:filters["filters"]
    :features:playlists["playlists"]
    :features:author["author"]
    :features:libraries["libraries"]
    :features:sessions["sessions"]
    :features:series["series"]
    :features:collections["collections"]
    :features:stats["stats"]
    :features:user["user"]
    :features:home["home"]
    :features:settings["settings"]
    :features:auth["auth"]
    :features:user["user"]
    :features:libraries["libraries"]
    :features:home["home"]
    :features:series["series"]
    :features:collections["collections"]
    :features:author["author"]
    :features:sessions["sessions"]
    :features:search["search"]
    :features:settings["settings"]
    :features:stats["stats"]
    :features:filters["filters"]
    :features:playlists["playlists"]
    :features:podcasts["podcasts"]
    :features:auth["auth"]
    :features:search["search"]
    :features:podcasts["podcasts"]
    subgraph :sessions
      :features:sessions:api["api"]
      :features:sessions:impl["impl"]
      :features:sessions:api["api"]
      :features:sessions:api["api"]
      :features:sessions:test["test"]
      :features:sessions:test["test"]
      :features:sessions:ui["ui"]
      :features:sessions:api["api"]
      :features:sessions:impl["impl"]
      :features:sessions:test["test"]
    end
    subgraph :settings
      :features:settings:api["api"]
      :features:settings:ui["ui"]
      :features:settings:test["test"]
      :features:settings:api["api"]
      :features:settings:impl["impl"]
      :features:settings:api["api"]
      :features:settings:test["test"]
      :features:settings:api["api"]
      :features:settings:impl["impl"]
      :features:settings:test["test"]
    end
    subgraph :libraries
      :features:libraries:api["api"]
      :features:libraries:impl["impl"]
      :features:libraries:api["api"]
      :features:libraries:test["test"]
      :features:libraries:ui["ui"]
      :features:libraries:test["test"]
      :features:libraries:api["api"]
      :features:libraries:impl["impl"]
      :features:libraries:test["test"]
      :features:libraries:api["api"]
    end
    subgraph :user
      :features:user:api["api"]
      :features:user:impl["impl"]
      :features:user:api["api"]
      :features:user:test["test"]
      :features:user:api["api"]
      :features:user:api["api"]
      :features:user:impl["impl"]
      :features:user:test["test"]
      :features:user:test["test"]
    end
    subgraph :podcasts
      :features:podcasts:impl["impl"]
      :features:podcasts:api["api"]
      :features:podcasts:ui["ui"]
      :features:podcasts:api["api"]
      :features:podcasts:api["api"]
      :features:podcasts:api["api"]
      :features:podcasts:impl["impl"]
    end
    subgraph :home
      :features:home:impl["impl"]
      :features:home:api["api"]
      :features:home:api["api"]
      :features:home:api["api"]
      :features:home:impl["impl"]
      :features:home:ui["ui"]
    end
    subgraph :playlists
      :features:playlists:ui["ui"]
      :features:playlists:api["api"]
      :features:playlists:api["api"]
      :features:playlists:impl["impl"]
      :features:playlists:api["api"]
      :features:playlists:api["api"]
      :features:playlists:impl["impl"]
    end
    subgraph :stats
      :features:stats:impl["impl"]
      :features:stats:api["api"]
      :features:stats:ui["ui"]
      :features:stats:api["api"]
      :features:stats:api["api"]
      :features:stats:api["api"]
      :features:stats:impl["impl"]
    end
    subgraph :filters
      :features:filters:impl["impl"]
      :features:filters:api["api"]
      :features:filters:api["api"]
      :features:filters:ui["ui"]
      :features:filters:test["test"]
      :features:filters:api["api"]
      :features:filters:test["test"]
      :features:filters:api["api"]
      :features:filters:impl["impl"]
      :features:filters:test["test"]
    end
    subgraph :author
      :features:author:impl["impl"]
      :features:author:api["api"]
      :features:author:api["api"]
      :features:author:ui["ui"]
      :features:author:api["api"]
      :features:author:impl["impl"]
      :features:author:api["api"]
    end
    subgraph :search
      :features:search:ui["ui"]
      :features:search:api["api"]
      :features:search:api["api"]
      :features:search:impl["impl"]
      :features:search:api["api"]
      :features:search:api["api"]
      :features:search:impl["impl"]
    end
    subgraph :collections
      :features:collections:api["api"]
      :features:collections:api["api"]
      :features:collections:ui["ui"]
      :features:collections:api["api"]
      :features:collections:impl["impl"]
      :features:collections:impl["impl"]
      :features:collections:api["api"]
    end
    subgraph :auth
      :features:auth:ui["ui"]
      :features:auth:api["api"]
      :features:auth:impl["impl"]
      :features:auth:api["api"]
      :features:auth:api["api"]
      :features:auth:api["api"]
      :features:auth:impl["impl"]
    end
    subgraph :series
      :features:series:api["api"]
      :features:series:test["test"]
      :features:series:test["test"]
      :features:series:api["api"]
      :features:series:api["api"]
      :features:series:impl["impl"]
      :features:series:test["test"]
      :features:series:api["api"]
      :features:series:ui["ui"]
      :features:series:impl["impl"]
    end
  end
  subgraph :app
    :app:android["android"]
    :app:baselineprofile["baselineprofile"]
    :app:baselineprofile["baselineprofile"]
    :app:android["android"]
    :app:desktop["desktop"]
    :app:ios["ios"]
    :app:android["android"]
    :app:desktop["desktop"]
    :app:ios["ios"]
    :app:baselineprofile["baselineprofile"]
  end
  subgraph :thirdparty
    :thirdparty:socketio-kotlin["socketio-kotlin"]
    :thirdparty:kmp-socketio["kmp-socketio"]
    :thirdparty:socketio-kotlin["socketio-kotlin"]
    :thirdparty:kmp-socketio["kmp-socketio"]
    :thirdparty:socketio-kotlin["socketio-kotlin"]
  end
  subgraph :scripts
    :scripts:app["app"]
    :scripts:app["app"]
  end

  :features:sessions:api --> :
  :features:sessions:impl --> :features:sessions:api
  :features:sessions:impl --> :features:settings:api
  :features:sessions:impl --> :features:libraries:api
  :features:sessions:impl --> :features:user:api
  :features:sessions:impl --> :
  :features:user:impl --> :features:user:api
  :features:user:impl --> :features:settings:api
  :features:user:impl --> :features:user:test
  :features:user:impl --> :
  :features:settings:ui --> :features:settings:api
  :features:settings:ui --> :features:libraries:api
  :features:settings:ui --> :
  :features:podcasts:impl --> :features:podcasts:api
  :features:podcasts:impl --> :features:user:api
  :features:podcasts:impl --> :
  :features --> :
  :features:home:impl --> :features:home:api
  :features:home:impl --> :features:settings:api
  :features:home:impl --> :features:user:api
  :features:home:impl --> :
  :app --> :
  :features:playlists:ui --> :features:playlists:api
  :features:playlists:ui --> :features:sessions:api
  :features:playlists:ui --> :
  :features:filters --> :
  :features:stats:impl --> :features:stats:api
  :features:stats:impl --> :features:user:api
  :features:stats:impl --> :
  :features:filters:impl --> :features:filters:api
  :features:filters:impl --> :features:user:api
  :features:filters:impl --> :
  :features:podcasts:ui --> :features:libraries:api
  :features:podcasts:ui --> :features:playlists:api
  :features:podcasts:ui --> :features:podcasts:api
  :features:podcasts:ui --> :features:user:api
  :features:podcasts:ui --> :features:sessions:api
  :features:podcasts:ui --> :features:user:test
  :features:podcasts:ui --> :
  :app:android --> :app:baselineprofile
  :app:android --> :
  :features:playlists --> :
  :features:libraries:impl --> :features:libraries:api
  :features:libraries:impl --> :features:settings:api
  :features:libraries:impl --> :features:user:api
  :features:libraries:impl --> :features:libraries:test
  :features:libraries:impl --> :
  :features:author:impl --> :features:author:api
  :features:author:impl --> :features:settings:api
  :features:author:impl --> :features:user:api
  :features:author:impl --> :
  :features:search:ui --> :features:search:api
  :features:search:ui --> :
  :thirdparty:socketio-kotlin --> :
  :features:collections:api --> :
  :features:auth:ui --> :features:auth:api
  :features:auth:ui --> :
  :features:libraries:ui --> :features:author:api
  :features:libraries:ui --> :features:collections:api
  :features:libraries:ui --> :features:libraries:api
  :features:libraries:ui --> :features:podcasts:api
  :features:libraries:ui --> :features:series:api
  :features:libraries:ui --> :features:sessions:api
  :features:libraries:ui --> :features:user:api
  :features:libraries:ui --> :features:filters:api
  :features:libraries:ui --> :features:playlists:api
  :features:libraries:ui --> :features:libraries:test
  :features:libraries:ui --> :features:sessions:test
  :features:libraries:ui --> :features:series:test
  :features:libraries:ui --> :features:settings:test
  :features:libraries:ui --> :features:user:test
  :features:libraries:ui --> :
  :features:author --> :
  :features:libraries --> :
  :features:sessions --> :
  :features:auth:impl --> :features:auth:api
  :features:auth:impl --> :features:settings:api
  :features:auth:impl --> :
  :thirdparty --> :
  :features:series --> :
  :features:playlists:api --> :
  :features:sessions:test --> :features:sessions:api
  :features:sessions:test --> :
  :features:settings:api --> :
  :features:collections --> :
  :features:series:test --> :features:series:api
  :features:series:test --> :
  :app:baselineprofile --> :
  :app:baselineprofile --> :app:android
  :features:podcasts:api --> :features:libraries:api
  :features:podcasts:api --> :
  :features:collections:ui --> :features:collections:api
  :features:collections:ui --> :
  :features:user:api --> :
  :features:search:api --> :
  :features:stats:ui --> :features:libraries:api
  :features:stats:ui --> :features:stats:api
  :features:stats:ui --> :
  :scripts --> :
  :thirdparty:kmp-socketio --> :thirdparty:socketio-kotlin
  :thirdparty:kmp-socketio --> :
  :features:filters:ui --> :features:filters:api
  :features:filters:ui --> :features:filters:test
  :features:filters:ui --> :
  :features:stats --> :
  :features:playlists:impl --> :features:playlists:api
  :features:playlists:impl --> :features:user:api
  :features:playlists:impl --> :
  :features:user --> :
  :features:home --> :
  :features:filters:api --> :
  :features:author:ui --> :features:author:api
  :features:author:ui --> :features:filters:api
  :features:author:ui --> :features:user:api
  :features:author:ui --> :
  :features:sessions:ui --> :features:sessions:api
  :features:sessions:ui --> :features:user:api
  :features:sessions:ui --> :features:libraries:api
  :features:sessions:ui --> :features:libraries:test
  :features:sessions:ui --> :features:sessions:test
  :features:sessions:ui --> :features:settings:test
  :features:sessions:ui --> :features:user:test
  :features:sessions:ui --> :
  :app:desktop --> :
  :app:ios --> :
  :features:settings:impl --> :features:settings:api
  :features:settings:impl --> :
  :features:home:api --> :
  :features:search:impl --> :features:search:api
  :features:search:impl --> :features:user:api
  :features:search:impl --> :features:settings:api
  :features:search:impl --> :
  :features:libraries:test --> :features:libraries:api
  :features:libraries:test --> :
  :features:settings:test --> :features:settings:api
  :features:settings:test --> :
  :features:stats:api --> :
  :features:auth:api --> :
  :features:filters:test --> :features:filters:api
  :features:filters:test --> :
  :features:settings --> :
  : --> :app
  : --> :app:android
  : --> :app:desktop
  : --> :app:ios
  : --> :app:baselineprofile
  : --> :features
  : --> :features:auth
  : --> :features:auth:api
  : --> :features:auth:impl
  : --> :features:user
  : --> :features:user:api
  : --> :features:user:impl
  : --> :features:user:test
  : --> :features:libraries
  : --> :features:libraries:api
  : --> :features:libraries:impl
  : --> :features:libraries:test
  : --> :features:home
  : --> :features:home:api
  : --> :features:home:impl
  : --> :features:series
  : --> :features:series:api
  : --> :features:series:impl
  : --> :features:series:test
  : --> :features:collections
  : --> :features:collections:api
  : --> :features:collections:impl
  : --> :features:author
  : --> :features:author:api
  : --> :features:author:impl
  : --> :features:sessions
  : --> :features:sessions:api
  : --> :features:sessions:impl
  : --> :features:sessions:test
  : --> :features:search
  : --> :features:search:api
  : --> :features:search:impl
  : --> :features:settings
  : --> :features:settings:api
  : --> :features:settings:impl
  : --> :features:settings:test
  : --> :features:stats
  : --> :features:stats:api
  : --> :features:stats:impl
  : --> :features:filters
  : --> :features:filters:api
  : --> :features:filters:impl
  : --> :features:filters:test
  : --> :features:playlists
  : --> :features:playlists:api
  : --> :features:playlists:impl
  : --> :features:podcasts
  : --> :features:podcasts:api
  : --> :features:podcasts:impl
  : --> :scripts
  : --> :scripts:app
  : --> :thirdparty
  : --> :thirdparty:kmp-socketio
  : --> :thirdparty:socketio-kotlin
  :features:auth --> :
  :features:series:api --> :
  :features:search --> :
  :scripts:app --> :
  :features:series:ui --> :features:series:api
  :features:series:ui --> :features:filters:api
  :features:series:ui --> :features:user:api
  :features:series:ui --> :
  :features:libraries:api --> :
  :features:podcasts --> :
  :features:series:impl --> :features:series:api
  :features:series:impl --> :features:settings:api
  :features:series:impl --> :features:user:api
  :features:series:impl --> :features:user:test
  :features:series:impl --> :
  :features:collections:impl --> :features:collections:api
  :features:collections:impl --> :features:settings:api
  :features:collections:impl --> :features:user:api
  :features:collections:impl --> :
  :features:user:test --> :features:user:api
  :features:user:test --> :
  :features:author:api --> :
  :features:home:ui --> :features:home:api
  :features:home:ui --> :features:libraries:api
  :features:home:ui --> :features:user:api
  :features:home:ui --> :features:user:test
  :features:home:ui --> :
```