import org.jetbrains.compose.ExperimentalComposeLibrary

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

        implementation(compose.components.resources)
        implementation(compose.components.uiToolingPreview)

        implementation(libs.circuitx.overlays)
        implementation(libs.compose.rich.text)
      }
    }

    commonTest {
      dependencies {
        implementation(projects.common.test)
        implementation(projects.data.analytics.test)
        implementation(projects.features.libraries.test)
        implementation(projects.features.sessions.test)
        implementation(projects.features.series.test)
        implementation(projects.features.settings.test)
        implementation(projects.features.user.test)
        implementation(projects.infra.audioplayer.test)

        implementation(libs.bundles.test.common)
        implementation(libs.bundles.test.ui)

        @OptIn(ExperimentalComposeLibrary::class)
        implementation(compose.uiTest)
      }
    }

    jvmMain {
      dependencies {
        implementation(compose.preview)
      }
    }

    jvmTest {
      dependencies {
        implementation(compose.desktop.currentOs)
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
