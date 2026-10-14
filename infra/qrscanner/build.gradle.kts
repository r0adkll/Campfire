// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

plugins {
  id("app.campfire.ui")
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        implementation(projects.core)
      }
    }

    val jvmCommon by creating {
      dependsOn(commonMain.get())

      dependencies {
        implementation(libs.zxing.core)
      }
    }

    jvmMain {
      dependsOn(jvmCommon)
    }

    androidMain {
      dependsOn(jvmCommon)

      dependencies {
        implementation(libs.accompanist.permissions)
        implementation(libs.androidx.camera.camera2)
        implementation(libs.androidx.camera.compose)
        implementation(libs.androidx.camera.lifecycle)
      }
    }
  }
}
