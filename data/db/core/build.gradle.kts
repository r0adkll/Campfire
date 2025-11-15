import app.campfire.convention.addKspDependencyForCommon

plugins {
  id("app.campfire.android.library")
  id("app.campfire.multiplatform")
  alias(libs.plugins.sqldelight)
  alias(libs.plugins.ksp)
}
@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
  compilerOptions {
    freeCompilerArgs.add("-opt-in=kotlin.uuid.ExperimentalUuidApi")
  }

  sqldelight {
    databases {
      create("CampfireDatabase") {
        packageName.set("app.campfire")
        schemaOutputDirectory.set(file("src/commonMain/sqldelight/app/campfire/databases"))
        generateAsync.set(true)
      }
    }
    linkSqlite.set(true)
  }

  sourceSets {
    commonMain {
      dependencies {
        implementation(projects.core)

        api(libs.sqldelight.coroutines)
        api(libs.sqldelight.async)
        api(libs.kotlinx.datetime)
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

addKspDependencyForCommon(libs.kimchi.compiler)
