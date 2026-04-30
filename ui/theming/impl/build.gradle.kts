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

        implementation(libs.compose.runtime)
        implementation(libs.compose.ui)

        implementation(libs.halogen.core)
        implementation(libs.halogen.compose)
        implementation(libs.halogen.engine)

        implementation(libs.stately.concurrent.collections)
        implementation(libs.sqldelight.coroutines)
        implementation(libs.sqldelight.async)
        implementation(libs.sqldelight.primitive)
      }
    }

    androidMain {
      dependencies {
        implementation(libs.sqldelight.android)
        implementation(libs.halogen.provider.nano)
        // Direct ML Kit GenAI dependency so DiagnosticGeminiNanoProvider can call
        // checkStatus() and surface the raw FeatureStatus to logcat.
        implementation("com.google.mlkit:genai-prompt:1.0.0-beta2")
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
