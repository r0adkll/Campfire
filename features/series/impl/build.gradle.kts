import app.campfire.convention.addKspDependencyForCommon

plugins {
  id("app.campfire.android.library")
  id("app.campfire.multiplatform")
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
  alias(libs.plugins.burst)
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
  sourceSets {
    commonMain {
      dependencies {
        api(projects.features.series.api)

        implementation(projects.features.settings.api)
        implementation(projects.core)
        implementation(projects.data.db.core)
        implementation(projects.data.network.api)
        implementation(projects.data.account.api)
        implementation(projects.data.db.mapping)
        implementation(projects.features.user.api)
        implementation(libs.store)
        implementation(libs.store.cache)
        implementation(libs.androidx.paging.common)
      }
    }

    commonTest {
      dependencies {
        implementation(projects.common.test)
        implementation(projects.data.account.test)
        implementation(projects.data.db.test)
        implementation(projects.data.network.test)
        implementation(projects.features.user.test)
        implementation(libs.bundles.test.common)
        implementation(libs.bundles.test.impl)
      }
    }
  }
}

addKspDependencyForCommon(libs.kimchi.compiler)
