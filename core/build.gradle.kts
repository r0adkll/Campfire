plugins {
  id("app.campfire.multiplatform")
  alias(libs.plugins.kotlin.serialization)
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
  sourceSets {
    commonMain {
      dependencies {
        api(libs.kimchi.annotations)
        api(libs.kotlinx.coroutines.core)
        api(libs.kotlininject.runtime)
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
