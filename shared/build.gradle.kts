import app.campfire.convention.addKspDependencyForAllTargets

plugins {
  id("app.campfire.android.library")
  id("app.campfire.multiplatform")
  id("app.campfire.compose")
  alias(libs.plugins.ksp)
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
  sourceSets {
    commonMain {
      dependencies {
        api(projects.core)
        api(projects.common.screens)
        api(projects.common.compose)

        // Data Modules
        api(projects.data.db)
        api(projects.data.network.impl)
        api(projects.data.account.impl)

        // Feature Modules
        api(projects.features.auth.impl)
        api(projects.features.auth.ui)

        api(projects.features.libraries.impl)
        api(projects.features.libraries.ui)

        api(compose.runtime)
        api(compose.foundation)
        api(compose.material)
        api(compose.material3)
        api(compose.materialIconsExtended)
        @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
        api(compose.components.resources)
        api(compose.ui)

        api(libs.circuit.foundation)
        api(libs.circuit.overlay)
        api(libs.circuit.runtime)
        api(libs.circuitx.gesturenav)
      }
    }

    androidMain {
      dependencies {
        api(libs.androidx.activity.activity)
        api(libs.androidx.activity.compose)
      }
    }
  }
}

android {
  sourceSets {
    named("main") {
      resources.srcDir("src/commonMain/resources")
    }
  }
}

addKspDependencyForAllTargets(libs.kotlininject.ksp)
addKspDependencyForAllTargets(libs.kimchi.compiler)
