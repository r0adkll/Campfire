// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

plugins {
  id("app.campfire.ui")
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        implementation(projects.features.filters.api)
        implementation(projects.features.libraries.api)
        implementation(projects.features.management.api)
        implementation(projects.features.user.api)

        implementation(libs.compose.rich.text)
      }
    }

    commonTest {
      dependencies {
        implementation(projects.data.analytics.test)
        implementation(projects.features.filters.test)
        implementation(projects.features.libraries.test)
        implementation(projects.features.user.test)
      }
    }
  }
}
