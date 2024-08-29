@file:Suppress("UnstableApiUsage")

plugins {
  id("app.campfire.android.application")
  id("app.campfire.kotlin.android")
  id("app.campfire.compose")
  alias(libs.plugins.ksp)
}

ksp {
  arg("me.tatarka.inject.generateCompanionExtensions", "true")
}

kotlin {
  compilerOptions {
    freeCompilerArgs.add("-Xopt-in=androidx.compose.material3.ExperimentalMaterial3Api")
  }
}

android {
  namespace = "app.campfire.android"

  defaultConfig {
    applicationId = "app.campfire.android"
    versionCode = 1
    versionName = "1.0.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }

  packaging {
    resources.excludes += setOf(
      // Exclude AndroidX version files
      "META-INF/*.version",
      // Exclude consumer proguard files
      "META-INF/proguard/*",
      // Exclude the Firebase/Fabric/other random properties files
      "/*.properties",
      "fabric/*.properties",
      "META-INF/*.properties",
      // License files
      "LICENSE*",
      // Exclude Kotlin unused files
      "META-INF/**/previous-compilation-data.bin",
    )
  }

  buildTypes {
    getByName("release") {
      isMinifyEnabled = false
    }
  }
}

dependencies {
  implementation(projects.shared)
  implementation(projects.common.screens)

  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.browser)

  implementation(libs.circuit.runtime)
  implementation(libs.circuit.foundation)

  ksp(libs.kimchi.compiler)
  ksp(libs.kotlininject.ksp)
}
