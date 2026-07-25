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
        api(projects.infra.socket.api)

        implementation(projects.core)
        implementation(projects.data.account.api)
        implementation(projects.features.settings.api)
        implementation(libs.kmp.socketio)
        // Forces kmp-socketio's transitive socketio-kotlin from 2.6.0 up to a version whose
        // packet regex tolerates line-terminator chars (U+2028 etc.) inside JSON payloads;
        // 2.6.0 throws InvalidSocketIOPacketException on such packets, crashing the app.
        // Safe to remove once kmp-socketio depends on socketio-kotlin >= 2.8.0 itself.
        implementation(libs.socketio.kotlin)
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.kotlinx.serialization.json)
      }
    }
    commonTest {
      dependencies {
        implementation(libs.bundles.test.common)
      }
    }
  }
}

addKspDependencyForAllTargets(libs.kimchi.compiler)
