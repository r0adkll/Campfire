plugins {
  id("app.campfire.ui")
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        api(projects.common.compose)
        api(projects.data.account.api)
        api(projects.data.account.ui)

        implementation(projects.features.settings.api)
        implementation(projects.infra.audioplayer.api)
        implementation(projects.infra.shake)

        implementation(libs.circuitx.overlays)

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
