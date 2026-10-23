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
        api(projects.features.reader.api)

        implementation(projects.core)
        implementation(projects.data.account.api)
        implementation(projects.data.db.core)
        implementation(projects.data.network.api)
        implementation(projects.features.libraries.api)
        implementation(projects.features.user.api)
        implementation(projects.infra.offline.api)

        implementation(libs.ktor.client.core)
        implementation(libs.kotlinx.io.core)
      }
    }

    commonTest {
      dependencies {
        implementation(projects.common.test)
        implementation(projects.features.libraries.test)
        implementation(projects.features.user.test)
        implementation(projects.data.db.test)
        implementation(projects.infra.offline.test)
        implementation(libs.ktor.client.mock)
        implementation(libs.bundles.test.common)
      }
    }
  }
}
