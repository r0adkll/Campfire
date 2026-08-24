// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

dependencyResolutionManagement {
  repositories {
    google()
    mavenCentral()
  }

  versionCatalogs {
    create("libs") {
      from(files("../libs.versions.toml"))
    }
  }
}

buildCache {
  val isCi = System.getenv().containsKey("CI")
  local {
    isEnabled = !isCi
  }
}

rootProject.name = "build-logic"
include(":convention")

// The :firebase module carries the proprietary Firebase Gradle plugins. F-Droid `scandelete`s
// its build.gradle.kts for FOSS builds, so include it only when present — an absent module then
// simply isn't a project (rather than an unresolvable empty one), and nothing references it
// because the app applies `app.campfire.firebase` under the same presence guard.
if (file("firebase/build.gradle.kts").exists()) {
  include(":firebase")
}
