// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

plugins {
  id("app.campfire.ui")
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        implementation(projects.data.bookinfo.api)
        implementation(projects.features.series.api)
        implementation(projects.features.filters.api)
        implementation(projects.features.user.api)
        implementation(projects.infra.audioplayer.api)
        implementation(projects.ui.appbar)
        implementation(projects.ui.navigation.api)

        implementation(libs.compose.components.resources)

        implementation(libs.androidx.paging.compose)
        // Need encodeUrlParameter() ext function
        implementation(libs.ktor.http)
      }
    }

    commonTest {
      dependencies {
        implementation(projects.data.analytics.test)
        implementation(projects.data.bookinfo.test)
        implementation(projects.features.series.test)
        implementation(projects.infra.audioplayer.test)
      }
    }
  }
}
