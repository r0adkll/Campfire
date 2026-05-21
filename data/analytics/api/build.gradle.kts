plugins {
  id("app.campfire.multiplatform")
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
  sourceSets {
    commonMain {
      dependencies {
        api(libs.kotlinx.atomicfu)
      }
    }

    commonTest {
      dependencies {
        implementation(projects.data.analytics.test)
        implementation(libs.bundles.test.common)
      }
    }
  }
}
