plugins {
  id("app.campfire.android.library")
  id("app.campfire.multiplatform")
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
  sourceSets {
    commonMain {
      dependencies {
        api(projects.core)

        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.kotlinx.datetime)
        implementation(libs.kotlinx.atomicfu)
      }
    }

    commonTest {
      dependencies {
        implementation(libs.kotlinx.coroutines.test)
        implementation(libs.kotlin.test)
        implementation(libs.turbine)
        implementation(libs.assertk)
      }
    }
  }
}
