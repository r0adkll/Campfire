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
        api(libs.coil.networking.ktor3)
        api(libs.compose.material3.expressive)
        api(libs.compose.material3.windowsizeclass)
        api(libs.materialcolorsutilities)
        api(libs.swatchbuckler.compose)
        api(libs.swatchbuckler.coil)

        api(compose.foundation)
        api(compose.material)
        api(compose.materialIconsExtended)
        api(compose.animation)
        api(compose.components.resources)
        api(compose.components.uiToolingPreview)
      }
    }

    val skikoMain by creating {
      dependsOn(commonMain.get())
    }

    val jvmCommon by creating {
      dependsOn(commonMain.get())

      dependencies {
        api(libs.ktor.client.okhttp)
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
        api(libs.androidx.paging.common)
        implementation(libs.androidx.activity.compose)

        implementation(compose.preview)
        implementation(compose.uiTooling)
      }
    }

    appleMain {
      dependsOn(skikoMain)
      dependencies {
        api(libs.ktor.client.darwin)
      }
    }
  }
}

addKspDependencyForCommon(libs.kimchi.compiler)
