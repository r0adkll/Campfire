@file:Suppress("UnstableApiUsage")

import com.google.firebase.appdistribution.gradle.firebaseAppDistribution

plugins {
  id("app.campfire.android.application")
  id("app.campfire.kotlin.android")
  id("app.campfire.compose")
  alias(libs.plugins.ksp)
  alias(libs.plugins.about.libraries)
  alias(libs.plugins.baselineprofile)
  alias(libs.plugins.google.services)
  alias(libs.plugins.firebase.crashlytics)
  alias(libs.plugins.firebase.appdistribution)
}

ksp {
  arg("me.tatarka.inject.generateCompanionExtensions", "true")
}

kotlin {
  compilerOptions {
    freeCompilerArgs.add("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")
  }
}

android {
  namespace = "app.campfire.android"

  defaultConfig {
    applicationId = "app.campfire.android"
    versionCode = properties["CAMPFIRE_VERSIONCODE"]?.toString()?.toIntOrNull() ?: 999999999
    versionName = properties["CAMPFIRE_VERSIONNAME"]?.toString() ?: "0.0.1"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }

  flavorDimensions += "default"

  productFlavors {
    create("standard")
    create("beta") {
      firebaseAppDistribution {
        artifactType = "APK"
        groups = "internal,external-public"
      }
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
        storeFile = file("../signing/campfire.keystore")
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
      signingConfig = signingConfigs.findByName("release") ?: signingConfigs["debug"]
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro",
      )
    }

    create("benchmarkRelease") {
      signingConfig = signingConfigs.findByName("release") ?: signingConfigs["debug"]
    }

    create("nonMinifiedRelease") {
      signingConfig = signingConfigs.findByName("release") ?: signingConfigs["debug"]
    }
  }
}

baselineProfile {
  dexLayoutOptimization = true
  saveInSrc = true
  mergeIntoMain = true
}

aboutLibraries {
  android.registerAndroidTasks = false
  export.prettyPrint = true
}

dependencies {
  implementation(platform(libs.google.firebase.bom))
  implementation(libs.google.firebase.analytics)
  implementation(libs.google.firebase.crashlytics)

  implementation(projects.app.common)
  implementation(projects.common.screens)

  implementation(libs.about.libraries.core)

  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.browser)
  implementation(libs.androidx.compose.ui)

  implementation(libs.circuit.runtime)
  implementation(libs.circuit.foundation)
  implementation(libs.androidx.profileinstaller)

  baselineProfile(projects.app.baselineprofile)
  implementation(libs.androidx.compose.runtime.tracing)

  debugImplementation(projects.infra.debug)

  "betaImplementation"(libs.google.firebase.appdistribution)
  "benchmarkReleaseImplementation"(libs.androidx.tracing.perfetto)
  "benchmarkReleaseImplementation"(libs.androidx.tracing.perfetto.binary)

  ksp(libs.kimchi.compiler)
  ksp(libs.kotlininject.ksp)
}
