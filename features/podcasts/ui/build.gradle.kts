plugins {
  id("app.campfire.ui")
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        implementation(projects.infra.audioplayer.api)
        implementation(projects.features.libraries.api)
        implementation(projects.features.playlists.api)
        implementation(projects.features.podcasts.api)
        implementation(projects.features.user.api)
        implementation(projects.features.sessions.api)
        implementation(projects.ui.appbar)
        implementation(projects.ui.navigation.api)
        implementation(projects.ui.theming.api)

        implementation(libs.compose.rich.text)
        implementation(libs.swatchbuckler.compose)
        implementation(libs.swatchbuckler.coil)
        implementation(libs.androidx.paging.common)
        implementation(libs.androidx.paging.compose)
      }
    }

    commonTest {
      dependencies {
        implementation(projects.common.test)
        implementation(projects.data.analytics.test)
        implementation(projects.features.user.test)
        implementation(libs.bundles.test.common)
        implementation(libs.bundles.test.ui)
      }
    }
  }
}
