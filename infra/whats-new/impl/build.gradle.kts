// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only
plugins {
  id("app.campfire.android.library")
  id("app.campfire.multiplatform")
  id("app.campfire.compose")
  id("app.campfire.changelog")
  id("app.campfire.di")
  alias(libs.plugins.kotlin.serialization)
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        api(projects.infra.whatsNew.api)

        implementation(projects.core)
        implementation(projects.features.settings.api)

        implementation(libs.kotlinx.serialization.json)

        implementation(libs.compose.components.resources)
      }
    }

    commonTest {
      dependencies {
        implementation(libs.bundles.test.common)
      }
    }
  }
}
