plugins {
  id("app.campfire.ui")
  alias(libs.plugins.burst)
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
        implementation(projects.ui.navigation)
        implementation(projects.ui.theming.api)

        implementation(libs.circuitx.overlays)
        implementation(libs.compose.rich.text)
        implementation(libs.swatchbuckler.compose)
        implementation(libs.swatchbuckler.coil)
      }
    }

    commonTest {
      dependencies {
        implementation(projects.data.analytics.test)
        implementation(projects.features.libraries.test)
        implementation(projects.features.sessions.test)
        implementation(projects.features.series.test)
        implementation(projects.features.settings.test)
        implementation(projects.features.user.test)
        implementation(projects.infra.audioplayer.test)
        implementation(projects.ui.theming.test)
      }
    }

    androidMain {
      dependencies {
        implementation(libs.accompanist.permissions)
      }
    }
  }
}
