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
        implementation(projects.core)
        api(projects.features.settings.api)
        api(libs.multiplatformsettings.core)
        api(libs.multiplatformsettings.coroutines)
        implementation(libs.androidx.datastore.preferences.core)
      }
    }

    commonTest {
      dependencies {
        implementation(libs.bundles.test.common)
        implementation(libs.bundles.test.impl)
        implementation(libs.multiplatformsettings.test)
      }
    }

    androidMain {
      dependencies {
        implementation(libs.androidx.preferences)
      }
    }
  }
}
