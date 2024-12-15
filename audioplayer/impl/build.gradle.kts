import app.campfire.convention.addKspDependencyForAllTargets

plugins {
  id("app.campfire.android.library")
  id("app.campfire.multiplatform")
  alias(libs.plugins.ksp)
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
  sourceSets {
    commonMain {
      dependencies {
        api(projects.audioplayer.api)

        implementation(projects.core)
        implementation(projects.common.settings)
        implementation(projects.data.account.api)
        implementation(projects.features.libraries.api)
        implementation(projects.features.sessions.api)
      }
    }

    androidMain {
      dependencies {
        api(libs.media3.exoplayer)
        implementation(libs.media3.exoplayer.hls)
        implementation(libs.media3.session)
        implementation(libs.media3.cast)
        implementation(libs.androidx.lifecycle.runtime)
      }
    }

    iosMain {
      dependencies {
      }
    }

    jvmMain {
      dependencies {
      }
    }
  }
}

addKspDependencyForAllTargets(libs.kimchi.compiler)
