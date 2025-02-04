import app.campfire.convention.addKspDependencyForCommon

plugins {
  id("app.campfire.multiplatform")
  alias(libs.plugins.ksp)
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        implementation(projects.core)
      }
    }
  }
}

addKspDependencyForCommon(libs.kimchi.compiler)
