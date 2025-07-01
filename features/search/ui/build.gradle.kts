plugins {
  id("app.campfire.ui")
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        implementation(projects.features.search.api)
        implementation(projects.infra.audioplayer.api)
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
