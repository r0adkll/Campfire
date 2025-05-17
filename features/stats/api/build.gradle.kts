plugins {
  id("app.campfire.multiplatform")
  alias(libs.plugins.metro)
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

// addKspDependencyForCommon(libs.kimchi.compiler)
