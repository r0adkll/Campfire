import app.campfire.convention.addKspDependencyForAllTargets

plugins {
  id("app.campfire.android.library")
  id("app.campfire.multiplatform")
  id("app.campfire.compose")
  alias(libs.plugins.ksp)
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {

  /**
   * NOTE: These have to match the iOS targets configured in
   * [app.campfire.convention.KotlinMultiplatformConventionPlugin]
   */
  listOf(
    iosArm64(),
    iosSimulatorArm64(),
  ).forEach { target ->
    target.compilations.getByName("main") {
      // https://kotlinlang.org/docs/multiplatform-dsl-reference.html#cinterops
      // The default file path is src/nativeInterop/cinterop/<interop-name>.def
      val nskeyvalueobserving by cinterops.creating
    }
  }

  sourceSets {
    commonMain {
      dependencies {
        api(projects.audioplayer.api)

        implementation(compose.runtime)

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
        implementation(libs.androidx.activity.compose)
      }
    }

    iosMain {
      dependencies {
        implementation(libs.ktor.client.core)
        implementation(libs.ktor.client.darwin)
      }
    }

    jvmMain {
      dependencies {
        implementation(libs.vlcj)
        implementation(libs.kotlinx.coroutines.swing)
      }
    }
  }
}

addKspDependencyForAllTargets(libs.kimchi.compiler)
