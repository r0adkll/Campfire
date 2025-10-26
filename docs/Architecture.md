# Architecture

This document provides an overview of the architecture decisions used in this project.

## Module Structure

This section describes the types of modules / groups that are used to build this application for all
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

### Split

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
└── public-ui/
    └── src/
        └── …
```

These are a group of modules for building features that provide function to other features/modules and ui/screens.
* `:api` - A lightweight module that can only depend on `:core` or other infra modules without other dependencies.
* `:impl` - The implementation module that provides the implementations and bindings for `:api`. This is only implemented by the `:app` module(s)
* `:ui` _[optional]_ - This module consumes `:api` and any other feature `:api` modules to provide Circuit screen implementations _(more on this later)_. This is only implemented by the `:app` module.
* `:public-ui` _[optional]_ - This should **_ONLY_** be used to share common ui components to other features that require logic specific to that module that wouldn't make it a good fit for the common ui/widgets module.

## Graph Structure

```mermaid
%%{
  init: {
    'theme': 'neutral'
  }
}%%

graph LR
  subgraph :app
    :app:android["android"]
    :app:baselineprofile["baselineprofile"]
    :app:common["common"]
    :app:desktop["desktop"]
    :app:ios["ios"]
  end
  subgraph :common
    :common:compose["compose"]
    :common:screens["screens"]
  end
  subgraph :data
    :data:db["db"]
    :data:mapping["mapping"]
  end
  subgraph :data:account
    :data:account:api["api"]
    :data:account:ui["ui"]
    :data:account:impl["impl"]
  end
  subgraph :data:analytics
    :data:analytics:impl["impl"]
    :data:analytics:api["api"]
    :data:analytics:mixpanel["mixpanel"]
  end
  subgraph :data:crashreporting
    :data:crashreporting:api["api"]
    :data:crashreporting:impl["impl"]
  end
  subgraph :data:network
    :data:network:api["api"]
    :data:network:impl["impl"]
  end
  subgraph :features:auth
    :features:auth:ui["ui"]
    :features:auth:api["api"]
    :features:auth:impl["impl"]
  end
  subgraph :features:author
    :features:author:impl["impl"]
    :features:author:api["api"]
    :features:author:ui["ui"]
  end
  subgraph :features:collections
    :features:collections:api["api"]
    :features:collections:ui["ui"]
    :features:collections:impl["impl"]
  end
  subgraph :features:home
    :features:home:impl["impl"]
    :features:home:api["api"]
    :features:home:ui["ui"]
  end
  subgraph :features:libraries
    :features:libraries:api["api"]
    :features:libraries:impl["impl"]
    :features:libraries:ui["ui"]
  end
  subgraph :features:search
    :features:search:ui["ui"]
    :features:search:api["api"]
    :features:search:impl["impl"]
  end
  subgraph :features:series
    :features:series:api["api"]
    :features:series:ui["ui"]
    :features:series:impl["impl"]
  end
  subgraph :features:sessions
    :features:sessions:api["api"]
    :features:sessions:impl["impl"]
    :features:sessions:ui["ui"]
  end
  subgraph :features:settings
    :features:settings:api["api"]
    :features:settings:ui["ui"]
    :features:settings:impl["impl"]
  end
  subgraph :features:stats
    :features:stats:impl["impl"]
    :features:stats:api["api"]
    :features:stats:ui["ui"]
  end
  subgraph :features:user
    :features:user:api["api"]
    :features:user:impl["impl"]
  end
  subgraph :infra
    :infra:shake["shake"]
    :infra:debug["debug"]
  end
  subgraph :infra:audioplayer
    :infra:audioplayer:api["api"]
    :infra:audioplayer:public-ui["public-ui"]
    :infra:audioplayer:impl["impl"]
  end
  subgraph :infra:updates
    :infra:updates:impl["impl"]
    :infra:updates:api["api"]
  end
  subgraph :ui
    :ui:attribution["attribution"]
    :ui:appbar["appbar"]
    :ui:drawer["drawer"]
  end
  subgraph :ui:widgets
    :ui:widgets:impl["impl"]
    :ui:widgets:api["api"]
  end
  :features:sessions:api --> :core
  :features:sessions:impl --> :features:sessions:api
  :features:sessions:impl --> :features:settings:api
  :features:sessions:impl --> :core
  :features:sessions:impl --> :data:db
  :features:sessions:impl --> :data:network:api
  :features:sessions:impl --> :data:account:api
  :features:sessions:impl --> :data:mapping
  :features:sessions:impl --> :features:libraries:api
  :features:sessions:impl --> :features:user:api
  :features:sessions:impl --> :infra:audioplayer:api
  :features:user:impl --> :features:user:api
  :features:user:impl --> :features:settings:api
  :features:user:impl --> :core
  :features:user:impl --> :data:db
  :features:user:impl --> :data:network:api
  :features:user:impl --> :data:account:api
  :features:user:impl --> :data:mapping
  :features:settings:ui --> :common:compose
  :features:settings:ui --> :data:account:api
  :features:settings:ui --> :data:account:ui
  :features:settings:ui --> :infra:audioplayer:public-ui
  :features:settings:ui --> :infra:audioplayer:api
  :features:settings:ui --> :features:settings:api
  :features:settings:ui --> :infra:shake
  :data:account:ui --> :common:compose
  :data:account:ui --> :data:account:api
  :data:account:ui --> :features:user:api
  :data:account:ui --> :features:libraries:api
  :infra:updates:impl --> :infra:updates:api
  :infra:updates:impl --> :core
  :infra:updates:impl --> :common:compose
  :ui:attribution --> :common:compose
  :infra:audioplayer:public-ui --> :common:compose
  :infra:audioplayer:public-ui --> :infra:audioplayer:api
  :features:home:impl --> :features:home:api
  :features:home:impl --> :features:settings:api
  :features:home:impl --> :core
  :features:home:impl --> :data:db
  :features:home:impl --> :data:mapping
  :features:home:impl --> :data:network:api
  :features:home:impl --> :data:account:api
  :features:home:impl --> :features:user:api
  :features:stats:impl --> :features:stats:api
  :features:stats:impl --> :core
  :features:stats:impl --> :data:db
  :features:stats:impl --> :data:network:api
  :features:stats:impl --> :data:account:api
  :features:stats:impl --> :data:mapping
  :features:stats:impl --> :features:user:api
  :data:account:impl --> :data:account:api
  :data:account:impl --> :features:settings:api
  :data:account:impl --> :data:db
  :data:account:impl --> :data:network:api
  :data:account:impl --> :data:mapping
  :data:account:impl --> :infra:audioplayer:api
  :data:account:impl --> :core
  :data:account:impl --> :features:user:api
  :data:account:impl --> :features:sessions:api
  :app:android --> :app:baselineprofile
  :app:android --> :infra:debug
  :app:android --> :app:common
  :app:android --> :common:screens
  :features:libraries:impl --> :features:libraries:api
  :features:libraries:impl --> :features:settings:api
  :features:libraries:impl --> :features:user:api
  :features:libraries:impl --> :core
  :features:libraries:impl --> :data:db
  :features:libraries:impl --> :data:network:api
  :features:libraries:impl --> :data:account:api
  :features:libraries:impl --> :data:mapping
  :data:analytics:impl --> :data:analytics:api
  :data:analytics:impl --> :core
  :data:analytics:impl --> :features:settings:api
  :features:author:impl --> :features:author:api
  :features:author:impl --> :features:settings:api
  :features:author:impl --> :core
  :features:author:impl --> :data:db
  :features:author:impl --> :data:network:api
  :features:author:impl --> :data:account:api
  :features:author:impl --> :data:mapping
  :features:author:impl --> :features:user:api
  :features:search:ui --> :common:compose
  :features:search:ui --> :features:search:api
  :features:search:ui --> :infra:audioplayer:api
  :features:search:ui --> :ui:appbar
  :common:compose --> :core
  :common:compose --> :common:screens
  :common:compose --> :features:settings:api
  :features:collections:api --> :core
  :features:auth:ui --> :common:compose
  :features:auth:ui --> :features:auth:api
  :features:auth:ui --> :data:account:api
  :features:libraries:ui --> :common:compose
  :features:libraries:ui --> :infra:audioplayer:api
  :features:libraries:ui --> :features:author:api
  :features:libraries:ui --> :features:collections:api
  :features:libraries:ui --> :features:libraries:api
  :features:libraries:ui --> :features:series:api
  :features:libraries:ui --> :features:sessions:api
  :features:libraries:ui --> :features:user:api
  :features:libraries:ui --> :ui:appbar
  :features:auth:impl --> :features:auth:api
  :features:auth:impl --> :features:settings:api
  :features:auth:impl --> :core
  :features:auth:impl --> :data:db
  :features:auth:impl --> :data:network:api
  :features:auth:impl --> :data:account:api
  :features:auth:impl --> :data:mapping
  :ui:drawer --> :common:compose
  :ui:drawer --> :data:account:api
  :ui:drawer --> :data:account:ui
  :ui:drawer --> :infra:updates:api
  :ui:drawer --> :features:libraries:api
  :core --> :data:analytics:api
  :features:settings:api --> :core
  :data:crashreporting:api --> :core
  :app:baselineprofile --> :app:android
  :infra:updates:api --> :core
  :infra:audioplayer:impl --> :infra:audioplayer:api
  :infra:audioplayer:impl --> :infra:shake
  :infra:audioplayer:impl --> :features:home:api
  :infra:audioplayer:impl --> :features:series:api
  :infra:audioplayer:impl --> :features:collections:api
  :infra:audioplayer:impl --> :features:author:api
  :infra:audioplayer:impl --> :features:search:api
  :infra:audioplayer:impl --> :core
  :infra:audioplayer:impl --> :features:settings:api
  :infra:audioplayer:impl --> :data:account:api
  :infra:audioplayer:impl --> :features:libraries:api
  :infra:audioplayer:impl --> :features:sessions:api
  :infra:audioplayer:impl --> :features:user:api
  :ui:widgets:impl --> :ui:widgets:api
  :ui:widgets:impl --> :features:home:api
  :ui:widgets:impl --> :core
  :ui:widgets:impl --> :common:compose
  :ui:widgets:impl --> :features:sessions:api
  :ui:widgets:impl --> :infra:audioplayer:api
  :features:collections:ui --> :common:compose
  :features:collections:ui --> :features:collections:api
  :features:collections:ui --> :infra:audioplayer:api
  :features:collections:ui --> :ui:appbar
  :features:user:api --> :core
  :features:search:api --> :core
  :features:stats:ui --> :common:compose
  :features:stats:ui --> :features:libraries:api
  :features:stats:ui --> :features:stats:api
  :ui:appbar --> :common:compose
  :ui:appbar --> :features:libraries:api
  :ui:appbar --> :data:account:api
  :data:account:api --> :core
  :data:network:impl --> :data:network:api
  :data:network:impl --> :features:settings:api
  :data:network:impl --> :core
  :data:network:impl --> :data:account:api
  :data:crashreporting:impl --> :data:crashreporting:api
  :data:crashreporting:impl --> :core
  :data:crashreporting:impl --> :features:settings:api
  :common:screens --> :core
  :features:author:ui --> :common:compose
  :features:author:ui --> :features:author:api
  :features:author:ui --> :infra:audioplayer:api
  :features:author:ui --> :ui:appbar
  :features:sessions:ui --> :common:compose
  :features:sessions:ui --> :features:sessions:api
  :features:sessions:ui --> :features:user:api
  :features:sessions:ui --> :features:libraries:api
  :features:sessions:ui --> :infra:audioplayer:api
  :features:sessions:ui --> :infra:audioplayer:public-ui
  :app:desktop --> :app:common
  :features:settings:impl --> :features:settings:api
  :features:settings:impl --> :core
  :app:ios --> :app:common
  :features:home:api --> :core
  :features:search:impl --> :features:search:api
  :features:search:impl --> :features:user:api
  :features:search:impl --> :features:settings:api
  :features:search:impl --> :core
  :features:search:impl --> :data:db
  :features:search:impl --> :data:network:api
  :features:search:impl --> :data:account:api
  :features:search:impl --> :data:mapping
  :data:db --> :core
  :features:stats:api --> :core
  :features:auth:api --> :core
  :features:auth:api --> :common:screens
  :infra:audioplayer:api --> :core
  :data:mapping --> :core
  :data:mapping --> :data:account:api
  :data:mapping --> :data:db
  :data:mapping --> :data:network:api
  :features:series:api --> :core
  :features:series:ui --> :common:compose
  :features:series:ui --> :features:series:api
  :features:series:ui --> :infra:audioplayer:api
  :features:series:ui --> :ui:appbar
  :features:libraries:api --> :core
  :features:libraries:api --> :common:screens
  :features:series:impl --> :features:series:api
  :features:series:impl --> :features:settings:api
  :features:series:impl --> :core
  :features:series:impl --> :data:db
  :features:series:impl --> :data:network:api
  :features:series:impl --> :data:account:api
  :features:series:impl --> :data:mapping
  :features:series:impl --> :features:user:api
  :infra:debug --> :common:compose
  :app:common --> :core
  :app:common --> :common:screens
  :app:common --> :common:compose
  :app:common --> :data:db
  :app:common --> :data:network:impl
  :app:common --> :data:account:impl
  :app:common --> :data:account:ui
  :app:common --> :data:analytics:impl
  :app:common --> :data:analytics:mixpanel
  :app:common --> :data:crashreporting:impl
  :app:common --> :infra:audioplayer:impl
  :app:common --> :infra:audioplayer:public-ui
  :app:common --> :infra:updates:impl
  :app:common --> :features:home:impl
  :app:common --> :features:home:ui
  :app:common --> :features:auth:impl
  :app:common --> :features:auth:ui
  :app:common --> :features:user:impl
  :app:common --> :features:libraries:impl
  :app:common --> :features:libraries:ui
  :app:common --> :features:series:impl
  :app:common --> :features:series:ui
  :app:common --> :features:collections:impl
  :app:common --> :features:collections:ui
  :app:common --> :features:author:impl
  :app:common --> :features:author:ui
  :app:common --> :features:sessions:impl
  :app:common --> :features:sessions:ui
  :app:common --> :features:search:impl
  :app:common --> :features:search:ui
  :app:common --> :features:settings:impl
  :app:common --> :features:settings:ui
  :app:common --> :features:stats:impl
  :app:common --> :features:stats:ui
  :app:common --> :ui:drawer
  :app:common --> :ui:attribution
  :app:common --> :ui:widgets:impl
  :data:analytics:mixpanel --> :data:analytics:api
  :data:analytics:mixpanel --> :core
  :data:analytics:mixpanel --> :features:settings:api
  :infra:shake --> :core
  :features:collections:impl --> :features:collections:api
  :features:collections:impl --> :features:settings:api
  :features:collections:impl --> :core
  :features:collections:impl --> :data:db
  :features:collections:impl --> :data:network:api
  :features:collections:impl --> :data:account:api
  :features:collections:impl --> :data:mapping
  :features:collections:impl --> :features:user:api
  :ui:widgets:api --> :core
  :features:author:api --> :core
  :features:home:ui --> :features:home:api
  :features:home:ui --> :features:libraries:api
  :features:home:ui --> :infra:audioplayer:api
  :features:home:ui --> :ui:appbar
  :features:home:ui --> :common:compose
```