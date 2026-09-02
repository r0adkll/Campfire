// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

plugins {
  id("app.campfire.ui")
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        implementation(projects.common.screens)
        implementation(projects.features.discover.api)

        implementation(libs.compose.components.resources)
        implementation(libs.kotlinx.datetime)
      }
    }

    commonTest {
      dependencies {
        implementation(projects.common.test)
        implementation(projects.data.bookinfo.test)
        implementation(libs.bundles.test.ui)
        implementation(libs.bundles.test.common)
      }
    }
  }
}
