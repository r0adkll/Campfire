plugins {
  id("app.campfire.ui")
  alias(libs.plugins.buildConfig)
}

buildConfig {
  packageName("app.campfire.auth.ui")
  buildConfigField("String?", "TEST_SERVER_URL", providers.gradleProperty("campfire_server_url").orNull)
  buildConfigField("String?", "TEST_USERNAME", providers.gradleProperty("campfire_username_2").orNull)
  buildConfigField("String?", "TEST_PASSWORD", providers.gradleProperty("campfire_password_2").orNull)
  useKotlinOutput()
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        implementation(projects.features.auth.api)
        implementation(projects.data.account.api)

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
