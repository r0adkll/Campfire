plugins {
  id("app.campfire.ui")
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        implementation(projects.features.libraries.api)

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
