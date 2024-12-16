plugins {
  id("app.campfire.ui")
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        implementation(projects.audioplayer.api)
        implementation(projects.features.libraries.api)
        implementation(projects.features.series.api)
        implementation(projects.features.sessions.api)
        implementation(projects.ui.appbar)

        implementation(compose.components.resources)

        implementation(libs.circuitx.overlays)
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
