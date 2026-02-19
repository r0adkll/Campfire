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

    androidMain {
      dependencies {
        implementation(libs.play.services.cast.framework)
      }
    }
  }
}
