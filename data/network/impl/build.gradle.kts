import app.campfire.convention.addKspDependencyForCommon

plugins {
  id("app.campfire.android.library")
  id("app.campfire.multiplatform")
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
  sourceSets {
    commonMain {
      dependencies {
        api(projects.data.network.api)
        implementation(projects.features.settings.api)
        implementation(projects.core)
        implementation(projects.data.account.api)

        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.kotlinx.datetime)
        implementation(libs.kotlinx.serialization.json)

        implementation(libs.ktor.client.core)
        implementation(libs.ktor.client.contentnegotiation)
        implementation(libs.ktor.client.logging)
        implementation(libs.ktor.client.serialization.json)

        api(libs.connectivity.core)
      }
    }

    commonTest {
      dependencies {
        implementation(libs.kotlin.test)
        implementation(libs.assertk)
      }
    }

    androidMain {
      dependencies {
        api(libs.okhttp.okhttp)
        api(libs.ktor.client.okhttp)
        implementation(libs.connectivity.android)
      }
    }

    jvmMain {
      dependencies {
        api(libs.okhttp.okhttp)
        api(libs.ktor.client.okhttp)
        implementation(libs.connectivity.http)
      }
    }

    iosMain {
      dependencies {
        api(libs.ktor.client.darwin)
        implementation(libs.connectivity.apple)
      }
    }
  }
}

addKspDependencyForCommon(libs.kimchi.compiler)
