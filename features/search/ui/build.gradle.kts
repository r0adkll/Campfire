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

        implementation(libs.compose.components.resources)

        implementation(libs.circuitx.overlays)
      }
    }

    jvmMain {
      dependencies {
        implementation(libs.compose.ui.tooling.preview)
      }
    }

    androidMain {
      dependencies {
        implementation(libs.compose.ui.tooling.preview)
      }
    }
  }
}
