// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

import com.android.build.api.dsl.ManagedVirtualDevice
import wtf.emulator.DeviceModel
import wtf.emulator.ewDevices

plugins {
  id("app.campfire.android.test")
  id("app.campfire.kotlin.android")
  alias(libs.plugins.baselineprofile)
  alias(libs.plugins.emulatorwtf)
}

// Baseline profiles are merged into :app:android's src/main (mergeIntoMain), so every flavor
// ships the same profile and the generator only needs to run against one of them. Pinning the
// test module to the standard flavor means a single emulator session instead of one per flavor.
// Set -Pcampfire.config.useEmulatorWtf=true (CI) to generate on emulator.wtf instead of a local
// Gradle managed device; it needs EW_API_TOKEN in the environment.
val useEmulatorWtf = providers.gradleProperty("campfire.config.useEmulatorWtf").orNull == "true"

android {
  namespace = "app.campfire.baselineprofile"

  defaultConfig {
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    missingDimensionStrategy("default", "standard")
  }

  targetProjectPath = ":app:android"

  // Devices used to generate baseline profiles, invoked via
  // ./gradlew :app:android:generateBaselineProfile
  testOptions.managedDevices {
    allDevices {
      @Suppress("UnstableApiUsage")
      create<ManagedVirtualDevice>("pixel6Api34") {
        device = "Pixel 6"
        apiLevel = 34
        systemImageSource = "google"
      }
    }
    ewDevices {
      register("ewPixel7Api34") {
        device.set(DeviceModel.PIXEL_7)
        apiLevel.set(34)
      }
    }
  }
}

// The maven.emulator.wtf repo (for the `ew-cli` runner) is registered by
// gradle/emulatorwtf-repo.gradle.kts, which F-Droid scandeletes for FOSS builds. Only enforce the
// plugin's startup repo check when that file is present; when it's absent (F-Droid) nothing runs
// emulator.wtf, so skip the check rather than fail configuration.
emulatorwtf {
  repositoryCheckEnabled.set(rootProject.file("gradle/emulatorwtf-repo.gradle.kts").exists())
}

// This is the configuration block for the Baseline Profile plugin.
// You can specify to run the generators on a managed devices or connected devices.
baselineProfile {
  managedDevices += if (useEmulatorWtf) "ewPixel7Api34" else "pixel6Api34"
  useConnectedDevices = false
  // Uncomment this to enable the emulator display for testing
  // enableEmulatorDisplay = true
}

dependencies {
  implementation(libs.androidx.test.ext.junit)
  implementation(libs.espresso.core)
  implementation(libs.androidx.uiautomator)
  implementation(libs.androidx.benchmark.macro.junit4)
  implementation(libs.androidx.tracing.perfetto)
  implementation(libs.androidx.tracing.perfetto.binary)
}

androidComponents {
  onVariants { v ->
    val artifactsLoader = v.artifacts.getBuiltArtifactsLoader()
    v.instrumentationRunnerArguments.put(
      "targetAppId",
      v.testedApks.map { artifactsLoader.load(it)?.applicationId!! },
    )
  }
}
