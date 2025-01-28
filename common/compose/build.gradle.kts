import app.campfire.convention.addKspDependencyForCommon

plugins {
  id("app.campfire.android.library")
  id("app.campfire.multiplatform")
  id("app.campfire.compose")
  alias(libs.plugins.ksp)
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        api(projects.core)
        api(projects.common.screens)
        api(projects.features.settings.api)

        api(libs.circuit.foundation)
        api(libs.circuit.overlay)
        api(libs.coil)
        api(libs.coil.compose)
        api(libs.coil.svg)
        api(libs.compose.material3.windowsizeclass)
        api(libs.materialcolorsutilities)

        api(compose.foundation)
        api(compose.material)
        api(compose.material3)
        api(compose.materialIconsExtended)
        api(compose.animation)
        api(compose.components.resources)
      }
    }

    val skikoMain by creating {
      dependsOn(commonMain.get())
    }

    val jvmCommon by creating {
      dependsOn(commonMain.get())

      dependencies {
        api(libs.coil.networking.okhttp)
      }
    }

    jvmMain {
      dependsOn(skikoMain)
      dependsOn(jvmCommon)
      dependencies {
        implementation(compose.preview)
      }
    }

    androidMain {
      dependsOn(jvmCommon)

      dependencies {
        api(libs.coil.networking.okhttp)
        api(libs.androidx.paging.common)
        implementation(libs.androidx.activity.compose)

        implementation(compose.preview)
        implementation(compose.uiTooling)
      }
    }

    appleMain {
      dependsOn(skikoMain)
      dependencies {
        api(libs.coil.networking.ktor3)
        api(libs.ktor.client.darwin)
      }
    }
  }
}

addKspDependencyForCommon(libs.kimchi.compiler)
