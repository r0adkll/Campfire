// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only
import app.campfire.convention.campfireBuildCommit

plugins {
  id("app.campfire.multiplatform")
  id("app.campfire.compose")
  id("app.campfire.di")
  alias(libs.plugins.about.libraries)
  alias(libs.plugins.buildConfig)
}

buildConfig {
  packageName("app.campfire.ios")
  campfireBuildCommit().declare { type, name, value -> buildConfigField(type, name, value) }
  useKotlinOutput { internalVisibility = true }
}

kotlin {
  compilerOptions {
    freeCompilerArgs.add("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")
  }

  sourceSets {
    commonMain {
      dependencies {
        implementation(projects.app.common)
        implementation(libs.compose.components.resources)
      }
    }
  }
}

aboutLibraries {
  export.prettyPrint = true
}
