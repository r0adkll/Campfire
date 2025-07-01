plugins {
  id("app.campfire.ui")
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        api(projects.features.home.api)
        api(projects.features.libraries.api)
        api(projects.infra.audioplayer.api)
        api(projects.ui.appbar)

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
