// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

import app.campfire.convention.addKspDependencyForCommon

plugins {
  id("app.campfire.multiplatform")
  alias(libs.plugins.ksp)
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        implementation(projects.core)

        api(libs.multiplatformsettings.core)
        api(libs.multiplatformsettings.coroutines)
      }
    }
  }
}

addKspDependencyForCommon(libs.kimchi.compiler)
