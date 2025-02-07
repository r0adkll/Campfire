import app.campfire.convention.addKspDependencyForCommon

plugins {
  id("app.campfire.android.library")
  id("app.campfire.multiplatform")
  alias(libs.plugins.ksp)
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
  sourceSets {
    commonMain {
      dependencies {
        api(projects.data.account.api)
        api(projects.features.settings.api)
        api(projects.data.db)
        api(projects.data.network.api)
        api(projects.data.mapping)

        implementation(projects.infra.audioplayer.api)
        implementation(projects.core)
        implementation(projects.features.user.api)
        implementation(projects.features.sessions.api)
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.multiplatformsettings.core)
        implementation(libs.multiplatformsettings.coroutines)
        implementation(libs.store)
      }
    }

    commonTest {
      dependencies {
        implementation(libs.kotlin.test)
      }
    }

    androidMain {
      dependencies {
        implementation(libs.androidx.preferences)
        implementation(libs.androidx.security.crypto)
      }
    }
  }
}

addKspDependencyForCommon(libs.kimchi.compiler)
