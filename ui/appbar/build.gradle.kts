plugins {
  id("app.campfire.ui")
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        api(projects.common.compose)
        api(projects.features.libraries.api)
        api(projects.features.search.api)
        api(projects.data.account.api)
        api(projects.ui.theming.api)

        implementation(libs.compose.components.resources)
      }
    }
  }
}
