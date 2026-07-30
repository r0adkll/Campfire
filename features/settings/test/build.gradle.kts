// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

plugins {
  id("app.campfire.multiplatform")
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
  sourceSets {
    commonMain {
      dependencies {
        api(projects.core)
        api(projects.features.settings.api)
        api(libs.multiplatformsettings.test)
        api(libs.kotlinx.coroutines.test)
      }
    }
  }
}
