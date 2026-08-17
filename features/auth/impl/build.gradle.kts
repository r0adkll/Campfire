// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

import app.campfire.convention.addKspDependencyForCommon

plugins {
  id("app.campfire.android.library")
  id("app.campfire.multiplatform")
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
  sourceSets {
    commonMain {
      dependencies {
        implementation(projects.features.settings.api)
        implementation(projects.core)
        implementation(projects.data.db.core)
        implementation(projects.data.network.api)
        implementation(projects.data.account.api)
        implementation(projects.data.db.mapping)

        implementation(libs.kotlinx.io.core)

        api(projects.features.auth.api)
      }
    }

    commonTest {
      dependencies {
        implementation(libs.bundles.test.common)
        implementation(libs.bundles.test.impl)
        implementation(libs.kotlinx.serialization.json)
      }
    }
  }
}

addKspDependencyForCommon(libs.kimchi.compiler)
