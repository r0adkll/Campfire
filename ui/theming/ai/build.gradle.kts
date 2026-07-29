plugins {
  id("app.campfire.ui")
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        api(projects.ui.theming.api)
        implementation(projects.ui.theming.ui)

        implementation(libs.compose.components.resources)

        implementation(libs.halogen.core)
        implementation(libs.halogen.compose)
        implementation(libs.halogen.engine)

        implementation(libs.cadence)
        implementation(libs.androidx.shapes)
      }
    }

    androidMain {
      dependencies {
        implementation(libs.halogen.provider.nano)
        // Direct ML Kit GenAI dependency so DiagnosticGeminiNanoProvider can call
        // checkStatus() and surface the raw FeatureStatus to logcat.
        implementation("com.google.mlkit:genai-prompt:1.0.0-beta4")
      }
    }
  }
}
