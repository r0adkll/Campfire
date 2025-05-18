plugins {
  id("app.campfire.android.library")
  id("app.campfire.multiplatform")
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.metro)
}

metro {
  enabled.set(false)
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
  sourceSets {
    commonMain {
      dependencies {
        api(libs.about.libraries.core)
        api(libs.kotlinx.coroutines.core)
        api(libs.kotlinx.datetime)
        api(libs.kotlinx.immutable)
        api(libs.uuid)

        implementation(libs.kotlinx.serialization.json)
      }
    }

    commonTest {
      dependencies {
        implementation(libs.kotlin.test)
      }
    }

    jvmTest {
      dependencies {
        implementation(libs.strikt.core)
      }
    }
  }
}

android {
  buildFeatures {
    buildConfig = true
  }
}
