// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

plugins {
  id("app.campfire.ui")
  alias(libs.plugins.buildConfig)
}

// Developer convenience: prefill the login form from ~/.gradle/gradle.properties. Builds that must
// not carry personal credentials (e.g. store screenshots) pass -Pcampfire_no_test_credentials=true.
val noTestCredentials = providers.gradleProperty("campfire_no_test_credentials").orNull == "true"
fun testCredential(name: String): String? =
  if (noTestCredentials) null else providers.gradleProperty(name).orNull

buildConfig {
  packageName("app.campfire.auth.ui")
  buildConfigField("String?", "TEST_SERVER_URL", testCredential("campfire_server_url"))
  buildConfigField("String?", "TEST_USERNAME", testCredential("campfire_username"))
  buildConfigField("String?", "TEST_PASSWORD", testCredential("campfire_password"))
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
