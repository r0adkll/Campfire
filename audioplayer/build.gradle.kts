import app.campfire.convention.addKspDependencyForCommon

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
        implementation(projects.core)
        implementation(projects.common.settings)
      }
    }

    androidMain {
      dependencies {
        implementation(libs.media3.exoplayer)
        implementation(libs.media3.exoplayer.hls)
        implementation(libs.media3.session)
        implementation(libs.media3.cast)
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

addKspDependencyForCommon(libs.kimchi.compiler)
