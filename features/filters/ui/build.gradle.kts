plugins {
  id("app.campfire.ui")
  alias(libs.plugins.burst)
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        implementation(projects.features.filters.api)
        implementation(libs.circuitx.overlays)
      }
    }

    commonTest {
      dependencies {
        implementation(projects.features.filters.test)
      }
    }
  }
}
