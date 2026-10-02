// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

plugins {
  id("app.campfire.android.library")
  id("app.campfire.multiplatform")
  id("app.campfire.parcelize")
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.burst)
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        api(libs.about.libraries.core)
        api(libs.kotlinx.coroutines.core)
        api(libs.metro.runtime)
        api(libs.kotlinx.datetime)
        api(libs.kotlinx.immutable)
        api(libs.stately.concurrent.collections)
        api(libs.uuid)

        api(projects.data.analytics.api)
        api(projects.infra.tracing)

        implementation(libs.kotlinx.serialization.json)
      }
    }

    commonTest {
      dependencies {
        implementation(libs.kotlin.test)
        implementation(libs.assertk)
      }
    }

    androidMain {
      dependencies {
        implementation(libs.androidx.activity.activity)
      }
    }

    jvmTest {
      dependencies {
        implementation(libs.strikt.core)
      }
    }
  }
}
