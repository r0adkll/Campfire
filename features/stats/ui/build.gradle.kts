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

        implementation(projects.features.libraries.api)
        implementation(projects.features.stats.api)

        implementation(libs.compose.components.resources)
      }
    }
  }
}
