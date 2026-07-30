// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

plugins {
  id("app.campfire.multiplatform")
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        api(projects.core)
        api(projects.infra.whatsNew.api)
        api(libs.kotlinx.coroutines.test)
      }
    }
  }
}
