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

        implementation(libs.about.libraries.compose.m3)
        implementation(libs.compose.components.resources)
      }
    }
  }
}
