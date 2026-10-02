// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only
plugins {
  id("app.campfire.android.library")
  id("app.campfire.multiplatform")
  alias(libs.plugins.kotlin.serialization)
  id("app.campfire.di")
  alias(libs.plugins.sqldelight)
}

kotlin {
  sqldelight {
    databases {
      create("BookInfoDatabase") {
        packageName.set("app.campfire.bookinfo.db")
        generateAsync.set(true)
      }
    }
    linkSqlite.set(true)
  }

  sourceSets {
    commonMain {
      dependencies {
        api(projects.data.bookinfo.api)

        implementation(projects.core)
        implementation(libs.kotlinx.serialization.json)
        implementation(libs.multiplatformsettings.core)
        implementation(libs.multiplatformsettings.coroutines)
        implementation(libs.sqldelight.coroutines)
        implementation(libs.sqldelight.async)
        implementation(libs.store)
      }
    }

    commonTest {
      dependencies {
        implementation(projects.common.test)
        implementation(projects.data.bookinfo.test)
        implementation(libs.bundles.test.common)
        implementation(libs.bundles.test.impl)
        implementation(libs.multiplatformsettings.test)
      }
    }

    androidMain {
      dependencies {
        implementation(libs.sqldelight.android)
      }
    }

    jvmMain {
      dependencies {
        implementation(libs.sqldelight.sqlite)
      }
    }

    nativeMain {
      dependencies {
        implementation(libs.sqldelight.native)
      }
    }
  }
}
