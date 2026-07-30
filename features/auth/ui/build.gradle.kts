// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

plugins {
  id("app.campfire.ui")
  alias(libs.plugins.buildConfig)
}

buildConfig {
  packageName("app.campfire.auth.ui")
  buildConfigField("String?", "TEST_SERVER_URL", providers.gradleProperty("campfire_server_url").orNull)
  buildConfigField("String?", "TEST_USERNAME", providers.gradleProperty("campfire_username").orNull)
  buildConfigField("String?", "TEST_PASSWORD", providers.gradleProperty("campfire_password").orNull)
  useKotlinOutput()
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        api(projects.data.network.oidc)
        implementation(projects.features.auth.api)
        implementation(projects.data.account.api)
        implementation(projects.ui.theming.api)

        implementation(libs.compose.components.resources)

        implementation(libs.circuitx.overlays)
      }
    }

    commonTest {
      dependencies {
        implementation(libs.bundles.test.common)
      }
    }
  }
}
