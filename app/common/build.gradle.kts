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
        api(projects.audioplayer.impl)
        api(projects.core)
        api(projects.common.screens)
        api(projects.common.compose)

        // Data Modules
        api(projects.data.db)
        api(projects.data.network.impl)
        api(projects.data.account.impl)
        api(projects.data.account.ui)

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

        api(projects.ui.drawer)
        api(projects.ui.attribution)

        api(compose.runtime)
        api(compose.foundation)
        api(compose.material)
        api(compose.material3)
        api(compose.material3AdaptiveNavigationSuite)
        api(compose.materialIconsExtended)
        @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
        api(compose.components.resources)
        api(compose.ui)

        api(libs.circuit.foundation)
        api(libs.circuit.overlay)
        api(libs.circuit.runtime)
        api(libs.circuitx.gesturenav)

        implementation(libs.kotlininject.runtime)
        implementation(libs.kimchi.annotations)
        implementation(libs.kimchi.circuit.annotations)
      }
    }
  }
}

ksp {
  arg("me.tatarka.inject.generateCompanionExtensions", "true")
}

android {
  sourceSets {
    named("main") {
      resources.srcDir("src/commonMain/resources")
    }
  }
}

addKspDependencyForAllTargets(libs.kotlininject.ksp)
addKspDependencyForAllTargets(libs.kimchi.compiler)
addKspDependencyForAllTargets(libs.kimchi.circuit.compiler)
