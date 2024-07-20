import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
  id("app.campfire.android.library")
  id("app.campfire.multiplatform")
  alias(libs.plugins.kotlin.parcelize)
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        api(projects.core)
        api(libs.circuit.runtime)
        api(libs.circuit.retained)
      }
    }
  }

  targets.configureEach {
    val isAndroidTarget = platformType == KotlinPlatformType.androidJvm
    compilations.configureEach {
      compileTaskProvider.configure {
        compilerOptions {
          if (isAndroidTarget) {
            freeCompilerArgs.addAll(
              "-P",
              "plugin:org.jetbrains.kotlin.parcelize:additionalAnnotation=app.campfire.common.screens.Parcelize",
            )
          }
        }
      }
    }
  }
}
