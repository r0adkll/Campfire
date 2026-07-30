// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

plugins {
  id("app.campfire.ui")
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        api(projects.common.compose)
        api(projects.features.libraries.api)
        api(projects.features.search.api)
        api(projects.data.account.api)
        api(projects.ui.theming.api)

        implementation(libs.compose.components.resources)
      }
    }
  }
}
