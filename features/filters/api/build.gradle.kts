// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

plugins {
  id("app.campfire.android.library")
  id("app.campfire.multiplatform")
  id("app.campfire.parcelize")
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        implementation(projects.core)

        implementation(libs.circuitx.overlays)
      }
    }
  }
}
