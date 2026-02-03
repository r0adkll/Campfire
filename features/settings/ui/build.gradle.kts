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
        api(projects.infra.audioplayer.publicUi)

        implementation(projects.data.analytics.api)
        implementation(projects.features.settings.api)
        implementation(projects.features.libraries.api)
        implementation(projects.infra.whatsNew.api)
        implementation(projects.infra.audioplayer.api)
        implementation(projects.infra.shake)
        implementation(projects.ui.theming.api)

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
