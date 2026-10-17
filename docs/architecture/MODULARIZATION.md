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

```