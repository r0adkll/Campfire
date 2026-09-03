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
        api(projects.features.stats.api)
        implementation(projects.core)
        implementation(projects.data.db.core)
        implementation(projects.data.network.api)
        implementation(projects.data.account.api)
        implementation(projects.data.db.mapping)
        implementation(projects.features.user.api)
      }
    }

    commonTest {
      dependencies {
        implementation(projects.data.account.test)
        implementation(projects.data.network.test)
        implementation(projects.features.user.test)
        implementation(libs.bundles.test.common)
        implementation(libs.bundles.test.impl)
      }
    }
  }
}

addKspDependencyForCommon(libs.kimchi.compiler)
