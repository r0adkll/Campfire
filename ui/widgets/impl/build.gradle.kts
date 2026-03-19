import app.campfire.convention.addKspDependencyForAllTargets

plugins {
  id("app.campfire.android.library")
  id("app.campfire.multiplatform")
  id("app.campfire.compose")
  alias(libs.plugins.ksp)
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
  android {
    namespace = "app.campfire.widgets"
  }

  sourceSets {
    commonMain {
      dependencies {
        api(projects.ui.widgets.api)

        implementation(projects.core)
        implementation(projects.common.compose)
        implementation(projects.features.sessions.api)
        implementation(projects.infra.audioplayer.api)

        implementation(libs.compose.runtime)
        implementation(libs.compose.ui)
      }
    }

    androidMain {
      dependencies {
        implementation(projects.features.home.api)

        implementation(libs.androidx.core.ktx)
        implementation(libs.androidx.glance.appwidget)
        implementation(libs.androidx.glance.material3)

        implementation(libs.coil)
        implementation(libs.coil.networking.okhttp)
      }
    }
  }
}

addKspDependencyForAllTargets(libs.kimchi.compiler)
