// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

import app.campfire.convention.addKspDependencyForCommon

plugins {
  id("app.campfire.android.library")
  id("app.campfire.multiplatform")
  id("app.campfire.compose")
  id("app.campfire.changelog")
  alias(libs.plugins.ksp)
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
  }
}

addKspDependencyForCommon(libs.kimchi.compiler)
