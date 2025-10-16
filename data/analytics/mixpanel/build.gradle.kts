import app.campfire.convention.addKspDependencyForCommon

plugins {
  id("app.campfire.android.library")
  id("app.campfire.multiplatform")
  alias(libs.plugins.ksp)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.buildConfig)
}

buildConfig {
  packageName("app.campfire.analytics.mixpanel")
  buildConfigField("String?", "MIXPANEL_TOKEN", providers.gradleProperty("mixpanel_token").orNull)
  useKotlinOutput()
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
  sourceSets {
    commonMain {
      dependencies {
        api(projects.data.analytics.api)

        implementation(projects.core)
        implementation(projects.features.settings.api)

        implementation(libs.kotlinx.serialization.json)
      }
    }

    commonTest {
      dependencies {
        implementation(libs.kotlin.test)
      }
    }

    androidMain {
      dependencies {
        api(libs.mixpanel.android)
      }
    }

    iosMain {
      dependencies {
      }
    }
  }
}

addKspDependencyForCommon(libs.kimchi.compiler)
