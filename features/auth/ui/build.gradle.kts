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

// -Pcampfire_test_server=<name> prefills the login form from a named credential set,
// campfire_<name>_server_url / _username / _password, instead of the default one (e.g. scripts/release
// signs the baseline profile generator in to a server that isn't behind Cloudflare Access).
val testServer = providers.gradleProperty("campfire_test_server").orNull?.takeIf { it.isNotBlank() }
fun testServerCredential(field: String): String? {
  if (testServer == null) return testCredential("campfire_$field")
  val name = "campfire_${testServer}_$field"
  return testCredential(name)
    ?: if (noTestCredentials) null else error("-Pcampfire_test_server=$testServer needs the Gradle property $name")
}

buildConfig {
  packageName("app.campfire.auth.ui")
  buildConfigField("String?", "TEST_SERVER_URL", testServerCredential("server_url"))
  buildConfigField("String?", "TEST_USERNAME", testServerCredential("username"))
  buildConfigField("String?", "TEST_PASSWORD", testServerCredential("password"))
  buildConfigField(
    "Int",
    "FAKE_RESTORABLE_ACCOUNTS",
    testCredential("campfire_fake_restorable_accounts")?.toIntOrNull() ?: 0,
  )
  useKotlinOutput()
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        api(projects.data.network.oidc)
        implementation(projects.features.auth.api)
        implementation(projects.infra.qrscanner)
        implementation(projects.data.account.api)
        implementation(projects.ui.theming.api)

        implementation(libs.compose.components.resources)

        implementation(libs.circuitx.overlays)
      }
    }

    commonTest {
      dependencies {
        implementation(libs.bundles.test.common)
        implementation(libs.bundles.test.ui)
      }
    }
  }
}
