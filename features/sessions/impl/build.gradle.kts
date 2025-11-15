import app.campfire.convention.addKspDependencyForCommon

plugins {
  id("app.campfire.android.library")
  id("app.campfire.multiplatform")
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
  compilerOptions {
    freeCompilerArgs.add("-opt-in=kotlin.uuid.ExperimentalUuidApi")
  }

  sourceSets {
    commonMain {
      dependencies {
        api(projects.features.sessions.api)

        implementation(projects.features.settings.api)
        implementation(projects.core)
        implementation(projects.data.db.core)
        implementation(projects.data.network.api)
        implementation(projects.data.account.api)
        implementation(projects.data.db.mapping)
        implementation(projects.features.libraries.api)
        implementation(projects.features.user.api)
        implementation(projects.infra.audioplayer.api)
        implementation(libs.store)
      }
    }
  }
}

addKspDependencyForCommon(libs.kimchi.compiler)
