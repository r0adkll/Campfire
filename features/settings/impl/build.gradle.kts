// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

import app.campfire.convention.addKspDependencyForCommon

plugins {
  id("app.campfire.android.library")
  id("app.campfire.multiplatform")
  alias(libs.plugins.ksp)
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        implementation(projects.core)
        api(projects.features.settings.api)
        api(libs.multiplatformsettings.core)
        api(libs.multiplatformsettings.coroutines)
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

addKspDependencyForCommon(libs.kimchi.compiler)
