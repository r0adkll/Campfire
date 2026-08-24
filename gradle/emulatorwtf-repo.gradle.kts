// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

// Registers the emulator.wtf Maven repository that hosts the `ew-cli` runner used when running
// tests / generating baseline profiles on emulator.wtf. The emulator.wtf Gradle plugin itself
// resolves from mavenCentral, so this repo is the only piece exclusive to maven.emulator.wtf and
// is never needed by a normal or FOSS build.
//
// It lives in a standalone script that settings.gradle.kts applies only if present, so F-Droid can
// `scandelete` it: any custom Maven repo declared in a .gradle(.kts) trips F-Droid's source
// scanner, and a FOSS build never runs emulator.wtf. Delete it and the repo simply isn't added.
dependencyResolutionManagement {
  repositories {
    maven("https://maven.emulator.wtf/releases/") {
      content { includeGroup("wtf.emulator") }
    }
  }
}
