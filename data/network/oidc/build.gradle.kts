// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

import app.campfire.convention.addKspDependencyForCommon

plugins {
  id("app.campfire.android.library")
  id("app.campfire.multiplatform")
  alias(libs.plugins.ksp)
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
  sourceSets {
    commonMain {
      dependencies {
        implementation(projects.core)
        implementation(projects.data.network.api)
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.ktor.client.core)

        implementation(libs.oidc.crypto)
      }
    }

    commonTest {
      dependencies {
        implementation(libs.kotlin.test)
      }
    }

    androidMain {
      dependencies {
        implementation(libs.androidx.activity.activity)
        implementation(libs.androidx.browser)
        implementation(libs.androidx.core.ktx)
      }
    }
  }
}

addKspDependencyForCommon(libs.kimchi.compiler)
