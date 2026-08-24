// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

plugins {
  `kotlin-dsl`
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(21))
  }
}

// This module isolates the proprietary Firebase Gradle plugins so a 100% FOSS build can drop
// them entirely: F-Droid `scandelete`s this build.gradle.kts, and the app only applies the
// `app.campfire.firebase` convention plugin when a google-services.json is present (never in a
// FOSS build), so the empty project that remains is never referenced.
dependencies {
  compileOnly(libs.android.gradlePlugin)
  implementation(libs.google.services.gradlePlugin)
  implementation(libs.firebase.crashlytics.gradlePlugin)
  implementation(libs.firebase.appdistribution.gradlePlugin)
}

gradlePlugin {
  plugins {
    register("firebase") {
      id = "app.campfire.firebase"
      implementationClass = "app.campfire.convention.FirebaseConventionPlugin"
    }
  }
}
