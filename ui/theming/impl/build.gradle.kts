import app.campfire.convention.addKspDependencyForAllTargets

plugins {
  id("app.campfire.android.library")
  id("app.campfire.multiplatform")
  id("app.campfire.compose")
  alias(libs.plugins.sqldelight)
  alias(libs.plugins.ksp)
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
  sqldelight {
    databases {
      create("CampfireThemeDatabase") {
        packageName.set("app.campfire.themes")
        schemaOutputDirectory.set(file("src/commonMain/sqldelight/app/campfire/schema"))
        generateAsync.set(true)
      }
    }
    linkSqlite.set(true)
  }

  sourceSets {
    commonMain {
      dependencies {
        api(projects.ui.theming.api)

        implementation(projects.core)
        implementation(projects.common.compose)
        implementation(projects.features.settings.api)

        implementation(compose.runtime)
        implementation(compose.ui)

        implementation(libs.stately.concurrent.collections)
        implementation(libs.sqldelight.coroutines)
        implementation(libs.sqldelight.async)
        implementation(libs.sqldelight.primitive)
      }
    }

    androidMain {
      dependencies {
        implementation(libs.sqldelight.android)
      }
    }

    iosMain {
      dependencies {
        implementation(libs.sqldelight.native)
      }
    }

    jvmMain {
      dependencies {
        implementation(libs.sqldelight.sqlite)
      }
    }
  }
}

addKspDependencyForAllTargets(libs.kimchi.compiler)
