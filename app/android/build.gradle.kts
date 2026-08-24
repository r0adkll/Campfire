// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

@file:Suppress("UnstableApiUsage")

import app.campfire.convention.campfireVersionCode
import app.campfire.convention.campfireVersionName
import app.campfire.convention.deriveVersionCode
import java.security.MessageDigest
import java.util.zip.Adler32

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

    // 100% FOSS build for F-Droid: same applicationId as standard, but the flavor-scoped
    // proprietary modules (Firebase, Mixpanel, ML Kit, Cast) are never wired in.
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
      it.name == "alphaRelease"
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

// Guards the source-of-truth invariant: campfire.versionCode must equal
// deriveVersionCode(campfire.version). The build reads campfire.versionCode directly, so a drift
// would silently ship the wrong code. Run in CI on every push (see build.yml).
tasks.register("verifyVersionCode") {
  group = "verification"
  description = "Assert campfire.versionCode == deriveVersionCode(campfire.version)."
  val versionName = providers.gradleProperty("campfire.version")
  val versionCode = providers.gradleProperty("campfire.versionCode")
  doLast {
    val name = versionName.get()
    val expected = deriveVersionCode(name)
      ?: error("campfire.version '$name' is not a valid semver (major.minor.patch[-rcN])")
    val actual = versionCode.orNull?.toIntOrNull()
      ?: error("campfire.versionCode is missing or not an integer")
    check(actual == expected) {
      "campfire.versionCode ($actual) != deriveVersionCode(campfire.version=$name) ($expected). " +
        "Update gradle.properties so the two agree."
    }
    logger.lifecycle("campfire.versionCode $actual matches campfire.version $name")
  }
}

// F-Droid does a byte-for-byte reproducible build of the foss flavor. R8 embeds a `pg-map-id`
// (a hash of the ProGuard/R8 mapping) in the DEX marker that varies by build environment even when
// the bytecode is identical — the sole thing that broke reproducibility. The mapping isn't published
// for the foss build, so the id is cosmetic: normalize it to a fixed value right after R8 and before
// packaging, so both our CI and F-Droid's rebuild produce identical DEX (and, since baseline.prof is
// compiled afterwards from this DEX, an identical profile too). standard/alpha are untouched.
//
// Inlined into the task action (no script-level helpers) so it stays configuration-cache compatible.
tasks.matching { it.name == "minifyFossReleaseWithR8" }.configureEach {
  val dexDir = layout.buildDirectory
    .dir("intermediates/dex/fossRelease/minifyFossReleaseWithR8").get().asFile
  val id = "0".repeat(64).toByteArray(Charsets.ISO_8859_1)
  doLast {
    val key = "\"pg-map-id\":\"".toByteArray(Charsets.ISO_8859_1)
    dexDir.listFiles { f -> f.name.matches(Regex("classes\\d*\\.dex")) }?.forEach { dexFile ->
      val bytes = dexFile.readBytes()
      var at = -1
      run {
        outer@ for (i in 0..bytes.size - key.size) {
          for (j in key.indices) if (bytes[i + j] != key[j]) continue@outer
          at = i; return@run
        }
      }
      if (at < 0) return@forEach
      val start = at + key.size
      var end = start
      while (end < bytes.size && bytes[end] != '"'.code.toByte()) end++
      require(end - start == id.size) { "unexpected pg-map-id length ${end - start}" }
      id.copyInto(bytes, start)
      // recompute DEX SHA-1 signature (bytes[32..] -> bytes[12..32]) and Adler-32 (bytes[12..] -> bytes[8..12])
      MessageDigest.getInstance("SHA-1").digest(bytes.copyOfRange(32, bytes.size)).copyInto(bytes, 12)
      val adler = Adler32().apply { update(bytes, 12, bytes.size - 12) }.value
      for (i in 0 until 4) bytes[8 + i] = ((adler shr (8 * i)) and 0xffL).toByte()
      dexFile.writeBytes(bytes)
    }
  }
}

baselineProfile {
  dexLayoutOptimization = true
  saveInSrc = true
  mergeIntoMain = true

  // F-Droid does a byte-for-byte reproducible build of the foss flavor and startup profiles break these.
  // (https://f-droid.org/docs/Reproducible_Builds/)
  variants {
    create("foss") {
      dexLayoutOptimization = false
    }
  }
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

  // Mixpanel analytics (opt-in, off by default)
  "standardImplementation"(projects.data.analytics.mixpanel)
  "alphaImplementation"(projects.data.analytics.mixpanel)

  // Google Cast remote playback (play-services-cast-framework + media3-cast)
  "standardImplementation"(projects.infra.audioplayer.cast)
  "alphaImplementation"(projects.infra.audioplayer.cast)

  // Firebase Crashlytics — release variants only, matching the previous src/release
  // FirebaseInitializer behavior (debug builds never initialize Firebase). These
  // variant-scoped configurations are created late by AGP, so they're added lazily.
  val crashlyticsVariants = setOf(
    "standardReleaseImplementation",
    "alphaReleaseImplementation",
  )
  configurations.matching { it.name in crashlyticsVariants }.configureEach {
    dependencies.add(project.dependencies.create(projects.data.crashreporting.firebase))
  }

  // Google Play in-app updates — only the Play-distributed production variant self-updates
  // through Play; alpha uses Firebase App Distribution and foss updates via its store.
  configurations.matching { it.name == "standardReleaseImplementation" }.configureEach {
    dependencies.add(project.dependencies.create(libs.google.play.app.update.get()))
  }

  implementation(libs.about.libraries.core)

  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.fragment)
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
