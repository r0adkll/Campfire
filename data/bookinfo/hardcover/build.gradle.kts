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
        api(projects.data.bookinfo.api)

        implementation(projects.core)
        implementation(projects.data.network.api)
        implementation(libs.ktor.client.core)
        implementation(libs.kotlinx.serialization.json)
        implementation(libs.multiplatformsettings.core)
        implementation(libs.multiplatformsettings.coroutines)
      }
    }

    commonTest {
      dependencies {
        implementation(projects.common.test)
        implementation(libs.bundles.test.common)
        implementation(libs.bundles.test.impl)
        implementation(libs.ktor.client.mock)
        implementation(libs.multiplatformsettings.test)
      }
    }

    androidMain {
      dependencies {
        implementation(libs.androidx.security.crypto)
      }
    }
  }
}

addKspDependencyForCommon(libs.kimchi.compiler)
