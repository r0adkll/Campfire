// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

plugins {
  id("app.campfire.ui")
  alias(libs.plugins.burst)
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        implementation(projects.features.filters.api)
        implementation(libs.circuitx.overlays)
      }
    }

    commonTest {
      dependencies {
        implementation(projects.features.filters.test)
      }
    }
  }
}
