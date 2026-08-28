// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

plugins {
  id("app.campfire.ui")
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        implementation(projects.features.sessions.api)
        implementation(projects.features.user.api)
        implementation(projects.features.libraries.api)
        implementation(projects.infra.audioplayer.api)
        implementation(projects.infra.audioplayer.publicUi)
        implementation(projects.ui.theming.api)

        implementation(libs.compose.components.resources)

        implementation(libs.circuitx.overlays)
        implementation(libs.compose.rich.text)
        implementation(libs.wavy.slider)
        implementation(libs.reorderable)
      }
    }

    commonTest {
      dependencies {
        implementation(projects.features.libraries.test)
        implementation(projects.features.sessions.test)
        implementation(projects.features.settings.test)
        implementation(projects.features.user.test)
        implementation(projects.infra.audioplayer.test)
        implementation(projects.ui.theming.test)
        implementation(libs.molecule)
      }
    }
  }
}
