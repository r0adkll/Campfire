plugins {
  id("app.campfire.ui")
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        implementation(projects.infra.audioplayer.api)
        implementation(projects.features.author.api)
        implementation(projects.features.collections.api)
        implementation(projects.features.libraries.api)
        implementation(projects.features.series.api)
        implementation(projects.features.sessions.api)
        implementation(projects.features.user.api)
        implementation(projects.ui.appbar)

        implementation(compose.components.resources)

        implementation(libs.circuitx.overlays)
        implementation(libs.compose.rich.text)
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
        implementation(libs.accompanist.permissions)
      }
    }
  }
}
