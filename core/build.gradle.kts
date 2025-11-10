plugins {
  id("app.campfire.android.library")
  id("app.campfire.multiplatform")
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.burst)
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
  sourceSets {
    commonMain {
      dependencies {
        api(libs.about.libraries.core)
        api(libs.kimchi.annotations)
        api(libs.kotlinx.coroutines.core)
        api(libs.kotlininject.runtime)
        api(libs.kotlinx.datetime)
        api(libs.kotlinx.immutable)
        api(libs.uuid)

        api(projects.data.analytics.api)

        implementation(libs.kotlinx.serialization.json)
      }
    }

    commonTest {
      dependencies {
        implementation(libs.kotlin.test)
        implementation(libs.assertk)
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
