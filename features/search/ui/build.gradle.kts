// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

plugins {
  id("app.campfire.ui")
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        implementation(projects.features.search.api)
        implementation(projects.infra.audioplayer.api)
        implementation(projects.ui.appbar)

        implementation(libs.compose.components.resources)

        implementation(libs.circuitx.overlays)
      }
    }
  }
}
