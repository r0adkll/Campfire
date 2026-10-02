// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only
plugins {
  id("app.campfire.android.library")
  id("app.campfire.multiplatform")
  alias(libs.plugins.sqldelight)
  id("app.campfire.di")
}

kotlin {
  compilerOptions {
    freeCompilerArgs.add("-opt-in=kotlin.uuid.ExperimentalUuidApi")
  }

  sqldelight {
    databases {
      create("CampfireDatabase") {
        packageName.set("app.campfire")
        schemaOutputDirectory.set(file("src/commonMain/sqldelight/app/campfire/databases"))
        generateAsync.set(true)
      }
    }
    linkSqlite.set(true)
  }

  sourceSets {
    commonMain {
      dependencies {
        implementation(projects.core)

        api(libs.androidx.paging.common)
        api(libs.sqldelight.coroutines)
        api(libs.sqldelight.async)
        api(libs.kotlinx.datetime)
        implementation(libs.kotlinx.serialization.json)
        implementation(libs.sqldelight.primitive)
      }
    }

    androidMain {
      dependencies {
        implementation(libs.sqldelight.android)
      }
    }

    iosMain {
      dependencies {
        implementation(libs.sqldelight.native)
      }
    }

    jvmMain {
      dependencies {
        implementation(libs.sqldelight.sqlite)
      }
    }

    jvmTest {
      dependencies {
        implementation(libs.bundles.test.common)
      }
    }
  }
}
