// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

@file:Suppress("UnstableApiUsage")

import app.campfire.convention.campfireAutomotiveVersionCode
import app.campfire.convention.campfireVersionName

plugins {
  id("app.campfire.android.application")
  id("app.campfire.kotlin.android")
  id("app.campfire.compose")
  id("app.campfire.di")
  alias(libs.plugins.about.libraries)
}

kotlin {
  compilerOptions {
    freeCompilerArgs.add("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")
  }
}

android {
  namespace = "app.campfire.automotive"

  defaultConfig {
    // The same package as the phone app, so both share one Play listing. The car build is
    // published on Play's Android Automotive OS track.
    applicationId = "app.campfire.android"

    versionCode = campfireAutomotiveVersionCode()
    versionName = campfireVersionName()
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }

  sourceSets {
    getByName("main") {
      // Launcher icons, theme and network security config are the phone app's
      res.directories.add("../android/src/main/res")
    }
  }

  signingConfigs {
    getByName("debug") {
      storeFile = rootProject.file("app/signing/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }

    if (rootProject.file("app/signing/campfire.keystore").exists()) {
      create("release") {
        storeFile = rootProject.file("app/signing/campfire.keystore")
        storePassword = properties["CAMPFIRE_KEYSTORE_PWD"]?.toString().orEmpty()
        keyAlias = "audiobooks"
        keyPassword = properties["CAMPFIRE_KEY_PWD"]?.toString().orEmpty()
      }
    }
  }

  buildTypes {
    debug {
      signingConfig = signingConfigs["debug"]
      versionNameSuffix = "-dev"
    }

    getByName("release") {
      val releaseSigning = signingConfigs.findByName("release")
      check(releaseSigning != null || properties["CAMPFIRE_VERSIONNAME"] == null) {
        "CAMPFIRE_VERSIONNAME is set but app/signing/campfire.keystore is missing; " +
          "refusing to sign a versioned release build with the debug key."
      }
      signingConfig = releaseSigning ?: signingConfigs["debug"]
      isMinifyEnabled = true
      isShrinkResources = true

      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        rootProject.file("app/android/base-proguard-rules.pro"),
        rootProject.file("app/android/prod-proguard-rules.pro"),
      )
    }
  }
}

aboutLibraries {
  export.prettyPrint = true
}

dependencies {
  implementation(projects.app.common)
  implementation(projects.common.screens)

  implementation(libs.about.libraries.core)

  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.ui)

  implementation(libs.circuit.runtime)
  implementation(libs.circuit.foundation)

  releaseImplementation(projects.data.crashreporting.firebase)
}
