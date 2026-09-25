// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

import app.campfire.convention.addKspDependencyForAllTargets

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
        api(projects.data.network.api)
        implementation(projects.features.settings.api)
        implementation(projects.core)
        implementation(projects.data.account.api)

        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.kotlinx.datetime)
        implementation(libs.kotlinx.serialization.json)

        implementation(libs.ktor.client.auth)
        implementation(libs.ktor.client.core)
        implementation(libs.ktor.client.contentnegotiation)
        implementation(libs.ktor.client.logging)
        implementation(libs.ktor.client.serialization.json)

        implementation(libs.livewire.plugin.network.ktor)

        api(libs.connectivity.core)
      }
    }

    commonTest {
      dependencies {
        implementation(libs.kotlin.test)
        implementation(libs.assertk)
        implementation(libs.kotlinx.coroutines.test)
        implementation(libs.ktor.client.mock)
        implementation(projects.features.settings.test)
        implementation(libs.turbine)
      }
    }

    androidMain {
      dependencies {
        api(libs.okhttp.okhttp)
        api(libs.ktor.client.okhttp)
      }
    }

    jvmMain {
      dependencies {
        api(libs.okhttp.okhttp)
        api(libs.ktor.client.okhttp)
        implementation(libs.connectivity.http)
      }
    }

    iosMain {
      dependencies {
        api(libs.ktor.client.darwin)
        implementation(libs.connectivity.apple)
      }
    }
  }
}

addKspDependencyForAllTargets(libs.kimchi.compiler)
