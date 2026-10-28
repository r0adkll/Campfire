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
        implementation(projects.data.crashreporting.api)
        implementation(projects.data.account.api)
        implementation(projects.data.db.core)
        implementation(projects.data.network.api)
        implementation(libs.store)
      }
    }

    commonTest {
      dependencies {
        implementation(projects.data.account.test)
        implementation(libs.kotlin.test)
        implementation(libs.assertk)
      }
    }
  }
}
