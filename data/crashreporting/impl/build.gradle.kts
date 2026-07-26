import app.campfire.convention.addKspDependencyForCommon

plugins {
  id("app.campfire.android.library")
  id("app.campfire.multiplatform")
  alias(libs.plugins.ksp)
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
  sourceSets {
    commonMain {
      dependencies {
        api(projects.data.crashreporting.api)

        implementation(projects.core)
        implementation(projects.features.settings.api)
      }
    }

    commonTest {
      dependencies {
        implementation(libs.kotlin.test)
      }
    }

    // Shared between the JVM and Android targets so throwable redaction (which needs
    // JVM stack trace APIs unavailable in commonMain) can be unit tested from jvmTest.
    val jvmShared by creating {
      dependsOn(commonMain.get())
    }

    jvmMain {
      dependsOn(jvmShared)
    }

    androidMain {
      dependsOn(jvmShared)
      dependencies {
        implementation(project.dependencies.platform(libs.google.firebase.bom))
        implementation(libs.google.firebase.crashlytics)
      }
    }
  }
}

addKspDependencyForCommon(libs.kimchi.compiler)
