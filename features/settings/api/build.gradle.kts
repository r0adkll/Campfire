import app.campfire.convention.addKspDependencyForCommon

plugins {
  id("app.campfire.multiplatform")
  alias(libs.plugins.ksp)
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        api(projects.features.settings.api)
        implementation(projects.core)
        api(libs.multiplatformsettings.core)
        api(libs.multiplatformsettings.coroutines)
      }
    }
  }
}

addKspDependencyForCommon(libs.kimchi.compiler)
