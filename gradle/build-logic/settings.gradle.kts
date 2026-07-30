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
