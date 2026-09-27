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
        api(projects.ui.navigation.api)

        implementation(projects.core)
        implementation(projects.common.compose)
        implementation(projects.infra.whatsNew.api)
        implementation(projects.ui.theming.api)
      }
    }
  }
}
