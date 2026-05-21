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
        implementation(projects.features.settings.api)
        implementation(projects.core)
        implementation(projects.data.db.core)
        implementation(projects.data.network.api)
        implementation(projects.data.account.api)
        implementation(projects.data.db.mapping)
        implementation(projects.infra.socket.api)

        implementation(libs.store)

        api(projects.features.user.api)
      }
    }

    commonTest {
      dependencies {
        implementation(projects.features.user.test)
        implementation(libs.bundles.test.common)
        implementation(libs.bundles.test.impl)
      }
    }
  }
}

addKspDependencyForCommon(libs.kimchi.compiler)
