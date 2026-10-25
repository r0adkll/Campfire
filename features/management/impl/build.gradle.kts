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
        api(projects.features.management.api)

        implementation(projects.core)
        implementation(projects.data.network.api)
        implementation(projects.features.libraries.api)

        implementation(libs.kotlinx.serialization.json)
      }
    }

    commonTest {
      dependencies {
        implementation(projects.data.network.test)
        implementation(libs.bundles.test.common)
        implementation(libs.bundles.test.impl)
      }
    }
  }
}
