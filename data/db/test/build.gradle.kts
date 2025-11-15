plugins {
  id("app.campfire.android.library")
  id("app.campfire.multiplatform")
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
  sourceSets {
    commonMain {
      dependencies {
        api(projects.data.db.core)
        api(projects.core)

        api(libs.sqldelight.coroutines)
        api(libs.sqldelight.async)
        api(libs.kotlinx.datetime)
        api(libs.burst.coroutines)
        implementation(libs.sqldelight.primitive)
        implementation(libs.kotlinx.coroutines.test)
      }
    }

    androidMain {
      dependencies {
        implementation(libs.sqldelight.sqlite)
      }
    }

    iosMain {
      dependencies {
        implementation(libs.sqldelight.native)
      }
    }

    jvmMain {
      dependencies {
        implementation(libs.sqldelight.sqlite)
      }
    }
  }
}
