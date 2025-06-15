import com.android.build.api.dsl.ManagedVirtualDevice

plugins {
  id("app.campfire.android.test")
  id("app.campfire.kotlin.android")
  alias(libs.plugins.baselineprofile)
}

android {
  namespace = "app.campfire.baselineprofile"

  defaultConfig {
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  targetProjectPath = ":app:android"

  // This code creates the gradle managed device used to generate baseline profiles.
  // To use GMD please invoke generation through the command line:
  // ./gradlew :app:android:generateBaselineProfile
  testOptions.managedDevices.allDevices {
    @Suppress("UnstableApiUsage")
    create<ManagedVirtualDevice>("pixel6Api34") {
      device = "Pixel 6"
      apiLevel = 34
      systemImageSource = "google"
    }
  }
}

// This is the configuration block for the Baseline Profile plugin.
// You can specify to run the generators on a managed devices or connected devices.
baselineProfile {
  managedDevices += "pixel6Api34"
  useConnectedDevices = false
}

dependencies {
  implementation(libs.androidx.test.ext.junit)
  implementation(libs.espresso.core)
  implementation(libs.androidx.uiautomator)
  implementation(libs.androidx.benchmark.macro.junit4)
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
