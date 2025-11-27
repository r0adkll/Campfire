plugins {
  id("app.campfire.ui")
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        api(projects.common.compose)
        api(projects.data.account.api)

        // FIXME: This is an unintended dependency cycle. We should probably extract this element
        //  or abstract it to projects.data.account.api
//        api(projects.data.account.ui)
        api(projects.infra.updates.api)
        api(projects.features.libraries.api)

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
