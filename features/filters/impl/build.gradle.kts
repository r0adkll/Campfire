// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only
plugins {
  id("app.campfire.android.library")
  id("app.campfire.multiplatform")
  id("app.campfire.di")
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
  sourceSets {
    commonMain {
      dependencies {
        api(projects.features.filters.api)
        api(projects.features.user.api)

        implementation(projects.core)
        implementation(projects.data.db.core)
        implementation(projects.data.network.api)
        implementation(projects.data.db.mapping)
        implementation(projects.data.crashreporting.api)

        implementation(libs.store)
      }
    }
  }
}
