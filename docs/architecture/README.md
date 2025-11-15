# Architecture

This document provides an overview of the architecture decisions used in this project. _TODO: Should describe more here really_

## Table of contents

1. [Modularization](MODULARIZATION.md)
2. [UI Layer](UI_LAYER.md)
3. [Data Layer](DATA_LAYER.md)

### [UI Layer](UI_LAYER.md)
Campfire uses [Circuit] to drive its entire UI/Presentation stack. Check out their documentation for more in depth details on the framework. Follow the link about for a more in-depth breakdown of the UI anatomy in this application.

```mermaid
%%{
  init: {
    'theme': 'dark'
  }
}%%

graph TB
  subgraph :ui
    MyScreen
    MyScreenUi
    MyScreenPresenter
    MyScreenUiState
    MyScreenUiEvent
  end
  subgraph :api
    LibraryItemRepository
  end
  subgraph :impl
    AudioBookShelfApi
    CampfireDatabase
    StoreLibraryItemRepository
  end

%% UI
  MyScreen --> MyScreenUi
  MyScreen --> MyScreenPresenter
  MyScreenUi --> MyScreenUiEvent
  MyScreenUiEvent --> MyScreenPresenter
  MyScreenPresenter --> MyScreenUiState
  MyScreenUiState --> MyScreenUi

%% DATA
  AudioBookShelfApi --> StoreLibraryItemRepository
  CampfireDatabase --> StoreLibraryItemRepository
  StoreLibraryItemRepository --> LibraryItemRepository
  LibraryItemRepository --> MyScreenPresenter
```

[Circuit]: https://slackhq.github.io/circuit/
