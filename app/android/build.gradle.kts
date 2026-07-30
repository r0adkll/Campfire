@file:Suppress("UnstableApiUsage")

import app.campfire.convention.campfireVersionCode
import app.campfire.convention.campfireVersionName

plugins {
  id("app.campfire.android.application")
  id("app.campfire.kotlin.android")
  id("app.campfire.compose")
  id("app.campfire.firebase")
  alias(libs.plugins.ksp)
  alias(libs.plugins.about.libraries)
  alias(libs.plugins.baselineprofile)
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

    versionCode = campfireVersionCode()
    versionName = campfireVersionName()

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }

  flavorDimensions += "default"

  productFlavors {
    create("standard")

    create("alpha") {
      applicationIdSuffix = ".alpha"
      versionNameSuffix = "-alpha"
    }

    create("beta") {
    }

    // 100% FOSS build for F-Droid / IzzyOnDroid: same applicationId as standard, but the
    // flavor-scoped proprietary modules (Firebase, Mixpanel, ML Kit) are never wired in.
    create("foss") {
    }
  }

  dependenciesInfo {
    // The Play-encrypted dependency metadata block is useless outside Google Play and
    // F-Droid flags APKs that carry it. Play consumes it from the AAB instead.
    includeInApk = false
    includeInBundle = true
  }

  sourceSets {
    matching {
      listOf("alphaRelease", "betaRelease").contains(it.name)
    }.configureEach {
      kotlin.directories.add("src/preRelease/kotlin")
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
      val releaseSigning = signingConfigs.findByName("release")
      // A versioned (tagged) release must never fall back to the committed debug key:
      // IzzyOnDroid rejects debug-signed APKs and users couldn't upgrade across keys.
      check(releaseSigning != null || properties["CAMPFIRE_VERSIONNAME"] == null) {
        "CAMPFIRE_VERSIONNAME is set but app/signing/campfire.keystore is missing; " +
          "refusing to sign a versioned release build with the debug key."
      }
      signingConfig = releaseSigning ?: signingConfigs["debug"]
      isMinifyEnabled = true
      isShrinkResources = true

      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "base-proguard-rules.pro",
        "prod-proguard-rules.pro",
      )
    }

    create("benchmarkRelease") {
      signingConfig = signingConfigs.findByName("release") ?: signingConfigs["debug"]
      setProguardFiles(
        listOf(
          getDefaultProguardFile("proguard-android-optimize.txt"),
          "base-proguard-rules.pro",
        ),
      )
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
  export.prettyPrint = true
}

dependencies {
  implementation(projects.app.common)
  implementation(projects.common.screens)

  // Proprietary integrations are wired per flavor so the foss flavor ships without them.

  // On-device AI theme generation (Gemini Nano via ML Kit)
  "standardImplementation"(projects.ui.theming.ai)
  "alphaImplementation"(projects.ui.theming.ai)
  "betaImplementation"(projects.ui.theming.ai)

  // Mixpanel analytics (opt-in, off by default)
  "standardImplementation"(projects.data.analytics.mixpanel)
  "alphaImplementation"(projects.data.analytics.mixpanel)
  "betaImplementation"(projects.data.analytics.mixpanel)

  // Google Cast remote playback (play-services-cast-framework + media3-cast)
  "standardImplementation"(projects.infra.audioplayer.cast)
  "alphaImplementation"(projects.infra.audioplayer.cast)
  "betaImplementation"(projects.infra.audioplayer.cast)

  // Firebase Crashlytics — release variants only, matching the previous src/release
  // FirebaseInitializer behavior (debug builds never initialize Firebase). These
  // variant-scoped configurations are created late by AGP, so they're added lazily.
  val crashlyticsVariants = setOf(
    "standardReleaseImplementation",
    "alphaReleaseImplementation",
    "betaReleaseImplementation",
  )
  configurations.matching { it.name in crashlyticsVariants }.configureEach {
    dependencies.add(project.dependencies.create(projects.data.crashreporting.firebase))
  }

  implementation(libs.about.libraries.core)

  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.browser)
  implementation(libs.androidx.compose.ui)

  implementation(libs.circuit.runtime)
  implementation(libs.circuit.foundation)
  implementation(libs.androidx.profileinstaller)

  baselineProfile(projects.app.baselineprofile)
  implementation(libs.androidx.compose.runtime.tracing)

  "benchmarkReleaseImplementation"(libs.androidx.tracing.perfetto)
  "benchmarkReleaseImplementation"(libs.androidx.tracing.perfetto.binary)

  debugImplementation(libs.androidx.tracing.perfetto)
  debugImplementation(libs.androidx.tracing.perfetto.binary)

  debugImplementation(libs.leakCanary)

  debugImplementation(libs.livewire.client)
  debugImplementation(libs.livewire.plugin.database)
  debugImplementation(libs.livewire.plugin.network.core)
  debugImplementation(libs.livewire.plugin.recomposition)

  debugImplementation(libs.androidx.lifecycle.process)
  debugImplementation(libs.media3.session)

  ksp(libs.kimchi.compiler)
  ksp(libs.kotlininject.ksp)
}
