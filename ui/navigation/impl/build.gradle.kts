// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

import app.campfire.convention.addKspDependencyForAllTargets

plugins {
  id("app.campfire.android.library")
  id("app.campfire.multiplatform")
  id("app.campfire.compose")
  alias(libs.plugins.ksp)
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        api(projects.ui.navigation.api)

        implementation(projects.core)
        implementation(projects.common.compose)
        implementation(projects.infra.whatsNew.api)
        implementation(projects.ui.theming.api)
      }
    }
  }
}

addKspDependencyForAllTargets(libs.kimchi.compiler)
