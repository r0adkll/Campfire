plugins {
  id("app.campfire.ui")
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        implementation(projects.features.sessions.api)
        implementation(projects.features.user.api)
        implementation(projects.features.libraries.api)
        implementation(projects.infra.audioplayer.api)
        implementation(projects.infra.audioplayer.publicUi)
        implementation(projects.ui.theming.api)

        implementation(compose.components.resources)

        implementation(libs.circuitx.overlays)
        implementation(libs.wavy.slider)
        implementation(libs.reorderable)
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
