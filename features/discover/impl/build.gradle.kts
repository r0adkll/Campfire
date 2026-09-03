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
        api(projects.features.discover.api)

        implementation(projects.core)
        implementation(projects.features.series.api)
        implementation(projects.data.bookinfo.api)

        implementation(libs.multiplatformsettings.core)
        implementation(libs.multiplatformsettings.coroutines)
      }
    }

    androidMain {
      dependencies {
        implementation(libs.androidx.core.ktx)
        implementation(libs.androidx.work.runtime)
      }
    }

    commonTest {
      dependencies {
        implementation(projects.common.test)
        implementation(projects.features.series.test)
        implementation(projects.data.bookinfo.test)
        implementation(libs.bundles.test.common)
        implementation(libs.multiplatformsettings.test)
      }
    }
  }
}

addKspDependencyForAllTargets(libs.kimchi.compiler)
