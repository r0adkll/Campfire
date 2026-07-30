// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

plugins {
  id("app.campfire.ui")
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        api(projects.features.home.api)
        api(projects.features.libraries.api)
        api(projects.features.user.api)
        api(projects.infra.audioplayer.api)
        api(projects.ui.appbar)
        api(projects.ui.navigation.api)

        implementation(libs.compose.components.resources)
      }
    }

    commonTest {
      dependencies {
        implementation(projects.common.test)
        implementation(projects.data.analytics.test)
        implementation(projects.features.user.test)
        implementation(projects.infra.audioplayer.test)
        implementation(libs.bundles.test.common)
        implementation(libs.bundles.test.ui)
      }
    }
  }
}
