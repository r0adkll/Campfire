// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only
plugins {
  id("app.campfire.android.library")
  id("app.campfire.multiplatform")
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        api(projects.data.crashreporting.api)

        implementation(projects.core)
        implementation(projects.features.settings.api)
      }
    }

    commonTest {
      dependencies {
        implementation(libs.kotlin.test)
      }
    }

    // Shared between the JVM and Android targets so throwable redaction (which needs
    // JVM stack trace APIs unavailable in commonMain) can be unit tested from jvmTest.
    val jvmShared by creating {
      dependsOn(commonMain.get())
    }

    jvmMain {
      dependsOn(jvmShared)
    }

    androidMain {
      dependsOn(jvmShared)
    }
  }
}
