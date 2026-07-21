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
        api(projects.infra.audioplayer.api)
        api(projects.infra.shake)

        implementation(libs.compose.runtime)
        implementation(libs.ktor.client.core)

        implementation(projects.core)
        implementation(projects.data.db.core)
        implementation(projects.features.settings.api)
        implementation(projects.data.account.api)
        implementation(projects.data.network.api)
        implementation(projects.data.crashreporting.api)
        implementation(projects.features.libraries.api)
        implementation(projects.features.sessions.api)
        implementation(projects.features.user.api)
      }
    }

    commonTest {
      dependencies {
        implementation(libs.kotlin.test)
        implementation(libs.assertk)
        implementation(projects.infra.audioplayer.test)
        implementation(projects.features.settings.test)
      }
    }

    androidMain {
      dependencies {
        api(libs.media3.exoplayer)
        api(libs.play.services.cast.framework)

        // These are used in the AudiPlayerService to facilitate
        // Android Auto support via the MediaLibraryService
        implementation(projects.features.home.api)
        implementation(projects.features.series.api)
        implementation(projects.features.playlists.api)
        implementation(projects.features.collections.api)
        implementation(projects.features.author.api)
        implementation(projects.features.search.api)

        implementation(libs.media3.exoplayer.hls)
        implementation(libs.media3.session)
        implementation(libs.media3.cast)
        implementation(libs.androidx.lifecycle.runtime)
        implementation(libs.androidx.activity.compose)
        implementation(libs.kotlinx.coroutines.guava)
        implementation(libs.coil)
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
