// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only
plugins {
  id("app.campfire.android.library")
  id("app.campfire.multiplatform")
  id("app.campfire.di")
  alias(libs.plugins.kotlin.serialization)
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        api(projects.data.account.api)
        api(projects.features.settings.api)
        api(projects.data.db.core)
        api(projects.data.network.api)
        api(projects.data.db.mapping)

        implementation(projects.infra.audioplayer.api)
        implementation(projects.core)
        implementation(projects.features.user.api)
        implementation(projects.features.sessions.api)
        implementation(projects.data.crashreporting.api)
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.kotlinx.serialization.json)
        implementation(libs.multiplatformsettings.core)
        implementation(libs.multiplatformsettings.coroutines)
        implementation(libs.store)
      }
    }

    commonTest {
      dependencies {
        implementation(libs.kotlin.test)
        implementation(libs.assertk)
        implementation(projects.common.test)
        implementation(projects.data.db.test)
        implementation(libs.bundles.test.impl)
        implementation(libs.multiplatformsettings.test)
        implementation(projects.features.settings.test)
      }
    }

    androidMain {
      dependencies {
        implementation(libs.androidx.preferences)
        implementation(projects.infra.securesettings)
      }
    }
  }
}
