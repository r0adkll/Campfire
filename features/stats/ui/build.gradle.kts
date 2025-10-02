plugins {
  id("app.campfire.ui")
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        api(projects.common.compose)

        implementation(projects.features.libraries.api)
        implementation(projects.features.stats.api)

        implementation(compose.components.resources)
      }
    }

    jvmMain {
      dependencies {
        implementation(compose.preview)
      }
    }

    androidMain {
      dependencies {
        implementation(compose.preview)
      }
    }
  }
}
