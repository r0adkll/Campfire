import app.campfire.convention.addKspDependencyForCommon

plugins {
  id("app.campfire.android.library")
  id("app.campfire.multiplatform")
  alias(libs.plugins.ksp)
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        api(projects.features.stats.api)
        implementation(projects.core)
        implementation(projects.data.db)
        implementation(projects.data.network.api)
        implementation(projects.data.account.api)
        implementation(projects.data.mapping)
      }
    }
  }
}

addKspDependencyForCommon(libs.kimchi.compiler)
