// Copyright 2023, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: Apache-2.0

package app.campfire.convention

import com.android.build.gradle.BaseExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

fun Project.configureAndroid(computeNamespace: Boolean = true) {
  android {
    if (computeNamespace) {
      namespace = "app.campfire.${path.substring(1).replace(':', '.').replace("-", "_")}"
    }
    compileSdkVersion(Versions.compileSdk)

    defaultConfig {
      minSdk = Versions.minSdk
      targetSdk = Versions.targetSdk

      testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
      // https://developer.android.com/studio/write/java8-support
      isCoreLibraryDesugaringEnabled = true
    }

    testOptions {
      unitTests.isReturnDefaultValues = true
    }
  }

  dependencies {
    // https://developer.android.com/studio/write/java8-support
    "coreLibraryDesugaring"(libs.findLibrary("tools.desugarjdklibs").get())
  }
}

private fun Project.android(action: BaseExtension.() -> Unit) = extensions.configure<BaseExtension>(action)
