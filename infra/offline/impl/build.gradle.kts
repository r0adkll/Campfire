// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

plugins {
  id("app.campfire.android.library")
  id("app.campfire.multiplatform")
  id("app.campfire.di")
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        api(projects.infra.offline.api)

        implementation(projects.data.network.api)
        implementation(projects.features.settings.api)
      }
    }

    commonTest {
      dependencies {
        implementation(libs.bundles.test.common)
        implementation(libs.kotlinx.coroutines.test)
        implementation(projects.features.settings.test)
      }
    }
  }
}
