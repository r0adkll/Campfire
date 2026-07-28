plugins {
  id("app.campfire.android.library")
  id("app.campfire.multiplatform")
  // Required to generate the serializer for EngineIOPacket.Open — without it the Engine.IO
  // handshake falls back to reflective serializer lookup and fails at runtime.
  alias(libs.plugins.kotlin.serialization)
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        api(libs.kotlinx.serialization.json)
        api(libs.kotlinx.io.bytestring)
      }
    }
    commonTest {
      dependencies {
        implementation(libs.kotlin.test)
      }
    }
  }
}
