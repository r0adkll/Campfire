// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

plugins {
  id("app.campfire.ui")
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        implementation(projects.data.analytics.api)
        implementation(projects.features.libraries.api)
        implementation(projects.infra.offline.api)
      }
    }

    commonTest {
      dependencies {
        implementation(projects.data.analytics.test)
        implementation(projects.features.libraries.test)
        implementation(projects.infra.offline.test)
      }
    }
  }
}
