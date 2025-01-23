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
        implementation(projects.core)
        api(projects.features.settings.api)
        api(libs.multiplatformsettings.core)
        api(libs.multiplatformsettings.coroutines)
      }
    }

    androidMain {
      dependencies {
        implementation(libs.androidx.preferences)
      }
    }
  }
}

addKspDependencyForCommon(libs.kimchi.compiler)
