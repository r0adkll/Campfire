// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

plugins {
  `kotlin-dsl`
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(21))
  }
}

dependencies {
  compileOnly(libs.android.gradlePlugin)
  compileOnly(libs.kotlin.gradlePlugin)
  compileOnly(libs.compose.gradlePlugin)
  compileOnly(libs.composeCompiler.gradlePlugin)
  compileOnly(libs.google.services.gradlePlugin)
  compileOnly(libs.firebase.crashlytics.gradlePlugin)
  compileOnly(libs.firebase.appdistribution.gradlePlugin)

  // JsonElement builder DSL only — no @Serializable classes, so the serialization
  // compiler plugin (which would have to version-match Gradle's embedded Kotlin)
  // isn't needed here.
  implementation(libs.kotlinx.serialization.json)

  testImplementation(libs.junit)
  testImplementation(libs.kotlin.test)
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

    register("changelog") {
      id = "app.campfire.changelog"
      implementationClass = "app.campfire.convention.changelog.ChangelogConventionPlugin"
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

    register("parcelize") {
      id = "app.campfire.parcelize"
      implementationClass = "app.campfire.convention.ParcelizeConventionPlugin"
    }

    register("firebase") {
      id = "app.campfire.firebase"
      implementationClass = "app.campfire.convention.FirebaseConventionPlugin"
    }
  }
}
