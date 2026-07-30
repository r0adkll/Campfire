// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

import app.campfire.convention.addKspDependencyForAllTargets

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
        api(projects.infra.socket.api)

        implementation(projects.core)
        implementation(projects.data.account.api)
        implementation(projects.features.settings.api)
        // Patched vendored copies of kmp-socketio and (transitively) socketio-kotlin — every
        // published socketio-kotlin's packet regex crashes on Audiobookshelf payloads, and the
        // published kmp-socketio lets decode exceptions escape its internal coroutines and crash
        // the app. See thirdparty/kmp-socketio/README.md and the vendored SocketIO.kt.
        implementation(projects.thirdparty.kmpSocketio)
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.kotlinx.serialization.json)
      }
    }
    commonTest {
      dependencies {
        implementation(libs.bundles.test.common)
      }
    }
  }
}

addKspDependencyForAllTargets(libs.kimchi.compiler)
