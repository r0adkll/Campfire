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

        api(libs.androidx.paging.compose)
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

        api(libs.compose.foundation)
        api(libs.compose.material)
        api(libs.compose.material.icons.extended)
        api(libs.compose.animation)
        api(libs.compose.components.resources)
        api(libs.compose.ui.tooling.preview)
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
    }

    androidMain {
      dependsOn(jvmCommon)

      dependencies {
        implementation(libs.accompanist.permissions)
        implementation(libs.androidx.activity.compose)
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

dependencies {
  debugImplementation(libs.compose.ui.tooling)
}

addKspDependencyForCommon(libs.kimchi.compiler)
