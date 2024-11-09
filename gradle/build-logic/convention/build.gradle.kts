// Copyright 2023, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: Apache-2.0


plugins {
  `kotlin-dsl`
  alias(libs.plugins.spotless)
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(17))
  }
}

spotless {
  kotlin {
    target("src/**/*.kt")
    ktlint(libs.versions.ktlint.get())
    licenseHeaderFile(rootProject.file("../../spotless/dh-copyright.txt"))
  }

  kotlinGradle {
    target("*.kts")
    ktlint(libs.versions.ktlint.get())
    licenseHeaderFile(rootProject.file("../../spotless/dh-copyright.txt"), "(^(?![\\/ ]\\**).*$)")
  }
}

dependencies {
  compileOnly(libs.android.gradlePlugin)
  compileOnly(libs.kotlin.gradlePlugin)
  compileOnly(libs.compose.gradlePlugin)
  compileOnly(libs.composeCompiler.gradlePlugin)
  compileOnly(libs.spotless.gradlePlugin)
}

gradlePlugin {
  plugins {
    register("root") {
      id = "app.campfire.root"
      implementationClass = "app.campfire.convention.RootConventionPlugin"
    }

    register("ui") {
      id = "app.campfire.ui"
      implementationClass = "app.campfire.convention.UiConventionPlugin"
    }

    register("compose") {
      id = "app.campfire.compose"
      implementationClass = "app.campfire.convention.ComposeConventionPlugin"
    }

    register("kotlinMultiplatform") {
      id = "app.campfire.multiplatform"
      implementationClass = "app.campfire.convention.KotlinMultiplatformConventionPlugin"
    }

    register("kotlinAndroid") {
      id = "app.campfire.kotlin.android"
      implementationClass = "app.campfire.convention.KotlinAndroidConventionPlugin"
    }

    register("kotlinJvm") {
      id = "app.campfire.kotlin.jvm"
      implementationClass = "app.campfire.convention.KotlinJvmConventionPlugin"
    }

    register("androidApplication") {
      id = "app.campfire.android.application"
      implementationClass = "app.campfire.convention.AndroidApplicationConventionPlugin"
    }

    register("androidLibrary") {
      id = "app.campfire.android.library"
      implementationClass = "app.campfire.convention.AndroidLibraryConventionPlugin"
    }

    register("androidTest") {
      id = "app.campfire.android.test"
      implementationClass = "app.campfire.convention.AndroidTestConventionPlugin"
    }
  }
}
