plugins {
  id("app.campfire.ui")
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        implementation(projects.features.series.api)
        implementation(projects.infra.audioplayer.api)
        implementation(projects.ui.appbar)

        implementation(compose.components.resources)

        // Need encodeUrlParameter() ext function
        implementation(libs.ktor.http)
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
