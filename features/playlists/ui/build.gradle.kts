// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

plugins {
  id("app.campfire.ui")
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        implementation(projects.features.playlists.api)
        implementation(projects.features.sessions.api)
        implementation(projects.infra.audioplayer.api)
        implementation(projects.ui.appbar)
        implementation(projects.ui.navigation.api)

        implementation(libs.circuitx.overlays)
        implementation(libs.reorderable)
      }
    }
  }
}
