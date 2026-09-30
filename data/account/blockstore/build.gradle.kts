// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only
plugins {
  id("app.campfire.android.library")
  id("app.campfire.multiplatform")
  alias(libs.plugins.kotlin.serialization)
  id("app.campfire.di")
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        implementation(projects.core)
        implementation(projects.data.account.api)
        implementation(libs.kotlinx.serialization.json)
      }
    }

    commonTest {
      dependencies {
        implementation(libs.bundles.test.common)
      }
    }

    androidMain {
      dependencies {
        implementation(projects.data.account.impl)

        implementation(libs.play.services.auth.blockstore)
        implementation(libs.kotlinx.coroutines.playservices)
      }
    }
  }
}
