// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

plugins {
  id("app.campfire.ui")
  alias(libs.plugins.kotlin.serialization)
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        implementation(projects.features.reader.api)
        implementation(projects.features.user.api)
        implementation(projects.infra.audioplayer.api)

        implementation(libs.kotlinx.serialization.json)
      }
    }

    androidMain {
      dependencies {
        implementation(libs.androidx.webkit)
      }
    }

    commonTest {
      dependencies {
        implementation(projects.features.settings.test)
        implementation(projects.features.user.test)
        implementation(projects.infra.audioplayer.test)
      }
    }
  }
}
