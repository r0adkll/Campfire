plugins {
  id("app.campfire.ui")
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        api(projects.common.compose)

        implementation(projects.infra.audioplayer.api)

        implementation(libs.circuitx.overlays)

        implementation(libs.compose.components.resources)
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
        implementation(libs.play.services.cast.framework)
      }
    }
  }
}
