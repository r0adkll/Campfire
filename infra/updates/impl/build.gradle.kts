// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only
plugins {
  id("app.campfire.android.library")
  id("app.campfire.multiplatform")
  id("app.campfire.compose")
  id("app.campfire.di")
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        api(projects.infra.updates.api)

        implementation(projects.core)
        implementation(projects.common.compose)
        implementation(projects.features.settings.api)

        implementation(libs.circuit.overlay)
        implementation(libs.circuitx.overlays)
        implementation(libs.compose.runtime)
        implementation(libs.compose.ui)
        implementation(libs.compose.components.resources)
      }
    }

    commonTest {
      dependencies {
        implementation(projects.features.settings.test)
        implementation(libs.bundles.test.common)
        implementation(libs.bundles.test.impl)
      }
    }

    jvmTest {
      dependencies {
        implementation(compose.desktop.currentOs)
      }
    }
  }
}
