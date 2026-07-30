// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.convention

import com.android.build.api.dsl.ApplicationBaseFlavor
import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

internal fun Project.configureAndroid(computeNamespace: Boolean = true) {
  extensions.configure<CommonExtension> {
    if (computeNamespace) {
      namespace = "app.campfire.${path.substring(1).replace(':', '.').replace("-", "_")}"
    }
    compileSdk { version = release(Versions.compileSdk) }

    with(defaultConfig) {
      minSdk = Versions.minSdk
      if (this is ApplicationBaseFlavor) {
        targetSdk = Versions.targetSdk
      }

      testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    with(compileOptions) {
      // https://developer.android.com/studio/write/java8-support
      isCoreLibraryDesugaringEnabled = true
    }

    with(testOptions) {
      unitTests.isReturnDefaultValues = true

      unitTests {
        isReturnDefaultValues = true
        all {
          // We make the assumption that any tests placed in
          // a 'composables' package are only intended to be ran as
          // instrumented ui tests
          it.exclude("**/composables/**")
        }
      }
    }
  }

  dependencies {
    // https://developer.android.com/studio/write/java8-support
    "coreLibraryDesugaring"(libs.findLibrary("tools.desugarjdklibs").get())
  }
}
