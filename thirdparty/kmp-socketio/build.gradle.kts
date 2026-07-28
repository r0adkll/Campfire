plugins {
  id("app.campfire.android.library")
  id("app.campfire.multiplatform")
}

kotlin {
  sourceSets {
    all {
      languageSettings.optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
    }
    commonMain {
      dependencies {
        api(projects.thirdparty.socketioKotlin)
        api(libs.kotlinx.coroutines.core)
        api(libs.ktor.client.core)
        api(libs.ktor.client.logging)
        api(libs.ktor.client.websockets)
        api(libs.kmp.xlog.api)
      }
    }
    jvmMain {
      dependencies {
        api(libs.ktor.client.cio)
      }
    }
    androidMain {
      dependencies {
        api(libs.ktor.client.okhttp)
      }
    }
    appleMain {
      dependencies {
        api(libs.ktor.client.darwin)
      }
    }
    commonTest {
      dependencies {
        implementation(libs.kotlin.test)
        implementation(libs.kotlinx.coroutines.test)
      }
    }
  }
}
