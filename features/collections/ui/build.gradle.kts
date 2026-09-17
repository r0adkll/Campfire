// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

plugins {
  id("app.campfire.ui")
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        implementation(projects.features.collections.api)
        implementation(projects.data.crashreporting.api)
        implementation(projects.infra.audioplayer.api)
        implementation(projects.ui.appbar)
        implementation(projects.ui.navigation.api)

        implementation(libs.compose.components.resources)

        implementation(libs.circuitx.overlays)
      }
    }

    commonTest {
      dependencies {
        implementation(projects.common.test)
        implementation(projects.data.analytics.test)
        implementation(projects.features.settings.test)
        implementation(libs.bundles.test.common)
        implementation(libs.bundles.test.ui)
      }
    }
  }
}
