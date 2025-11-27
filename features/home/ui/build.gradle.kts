plugins {
  id("app.campfire.ui")
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        api(projects.features.home.api)
        api(projects.features.libraries.api)
        api(projects.infra.audioplayer.api)
        api(projects.ui.appbar)
        api(projects.ui.navigation)

        implementation(compose.components.resources)
      }
    }

    commonTest {
      dependencies {
        implementation(projects.common.test)
        implementation(projects.data.analytics.test)
        implementation(projects.infra.audioplayer.test)
        implementation(libs.bundles.test.common)
        implementation(libs.bundles.test.ui)
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
