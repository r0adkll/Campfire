import app.campfire.convention.addKspDependencyForAllTargets

plugins {
  id("app.campfire.android.library")
  id("app.campfire.multiplatform")
  alias(libs.plugins.ksp)
}

kotlin {
  sourceSets {
    androidMain {
      dependencies {
        api(projects.data.crashreporting.impl)

        implementation(projects.core)
        implementation(projects.features.settings.api)

        implementation(project.dependencies.platform(libs.google.firebase.bom))
        implementation(libs.google.firebase.crashlytics)
      }
    }
  }
}

addKspDependencyForAllTargets(libs.kimchi.compiler)
