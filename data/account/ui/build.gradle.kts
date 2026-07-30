// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

plugins {
  id("app.campfire.ui")
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        implementation(projects.data.account.api)
        implementation(projects.features.user.api)
        implementation(projects.features.libraries.api)
        implementation(projects.ui.theming.api)
        implementation(projects.infra.socket.api)

        implementation(libs.compose.components.resources)

        implementation(libs.circuitx.overlays)
      }
    }
  }
}
