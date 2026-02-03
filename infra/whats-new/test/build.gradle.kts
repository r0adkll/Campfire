plugins {
  id("app.campfire.multiplatform")
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        api(projects.core)
        api(projects.infra.whatsNew.api)
        api(libs.kotlinx.coroutines.test)
      }
    }
  }
}
