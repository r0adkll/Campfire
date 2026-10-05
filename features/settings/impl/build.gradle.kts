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
      }
    }

    commonTest {
      dependencies {
        implementation(libs.bundles.test.common)
        implementation(libs.bundles.test.impl)
        implementation(libs.multiplatformsettings.test)
      }
    }

    // Android and desktop keep settings in FlatPrefs; iOS stays on NSUserDefaults
    val jvmShared by creating {
      dependsOn(commonMain.get())
      dependencies {
        implementation(libs.flatprefs.core)
      }
    }

    jvmMain {
      dependsOn(jvmShared)
    }

    androidMain {
      dependsOn(jvmShared)
    }
  }
}
