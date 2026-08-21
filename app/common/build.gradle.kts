// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

import app.campfire.convention.addKspDependencyForAllTargets

plugins {
  id("app.campfire.android.library")
  id("app.campfire.multiplatform")
  id("app.campfire.compose")
  alias(libs.plugins.ksp)
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
  sourceSets {
    commonMain {
      dependencies {
        api(projects.core)
        api(projects.common.screens)
        api(projects.common.compose)

        // Data Modules
        api(projects.data.db.core)
        api(projects.data.network.impl)
        api(projects.data.network.oidc)
        api(projects.data.account.impl)
        api(projects.data.account.ui)
        api(projects.data.analytics.impl)
        api(projects.data.crashreporting.impl)

        // Infra Modules
        api(projects.infra.audioplayer.impl)
        api(projects.infra.audioplayer.publicUi)
        api(projects.infra.updates.impl)
        api(projects.infra.whatsNew.impl)
        api(projects.infra.whatsNew.ui)
        api(projects.infra.tracing)
        api(projects.infra.socket.impl)

        // Feature Modules
        api(projects.features.home.impl)
        api(projects.features.home.ui)

        api(projects.features.auth.impl)
        api(projects.features.auth.ui)

        api(projects.features.user.impl)

        api(projects.features.libraries.impl)
        api(projects.features.libraries.ui)

        api(projects.features.series.impl)
        api(projects.features.series.ui)

        api(projects.features.collections.impl)
        api(projects.features.collections.ui)

        api(projects.features.author.impl)
        api(projects.features.author.ui)

        api(projects.features.sessions.impl)
        api(projects.features.sessions.ui)

        api(projects.features.search.impl)
        api(projects.features.search.ui)

        api(projects.features.settings.impl)
        api(projects.features.settings.ui)

        api(projects.features.stats.impl)
        api(projects.features.stats.ui)

        api(projects.features.filters.impl)
        api(projects.features.filters.ui)

        api(projects.features.playlists.impl)
        api(projects.features.playlists.ui)

        api(projects.features.podcasts.impl)
        api(projects.features.podcasts.ui)

        api(projects.ui.navigation.ui)
        api(projects.ui.navigation.impl)
        api(projects.ui.attribution)
        api(projects.ui.widgets.impl)
        api(projects.ui.theming.impl)
        api(projects.ui.theming.ui)

        api(libs.compose.runtime)
        api(libs.compose.foundation)
        api(libs.compose.material)
//        api(compose.material3)
        api(libs.compose.material3.expressive)
        api(libs.compose.material3.adaptive.navigation.suite)
        api(libs.compose.material.icons.extended)
        api(libs.compose.components.resources)
        api(libs.compose.ui)

        api(libs.circuit.foundation)
        api(libs.circuit.overlay)
        api(libs.circuit.runtime)
        api(libs.circuitx.gesturenav)
        api(libs.circuitx.navigation)

        api(libs.coil.networking.ktor3)
        api(libs.ktor.client.auth)

        implementation(libs.kotlininject.runtime)
        implementation(libs.kimchi.annotations)
        implementation(libs.kimchi.circuit.annotations)
        implementation(libs.compose.navigationevent)
      }
    }

    commonTest {
      dependencies {
        implementation(libs.kotlin.test)
        implementation(libs.assertk)
        implementation(libs.kotlinx.coroutines.test)
      }
    }

    androidMain {
      dependencies {
        implementation(libs.androidx.lifecycle.process)
      }
    }
  }
}

ksp {
  arg("me.tatarka.inject.generateCompanionExtensions", "true")
}

addKspDependencyForAllTargets(libs.kotlininject.ksp)
addKspDependencyForAllTargets(libs.kimchi.compiler)
addKspDependencyForAllTargets(libs.kimchi.circuit.compiler)
