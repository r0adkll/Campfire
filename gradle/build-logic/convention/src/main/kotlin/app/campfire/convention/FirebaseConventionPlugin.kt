// Copyright 2023, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: Apache-2.0

package app.campfire.convention

import com.android.build.api.dsl.ApplicationExtension
import com.google.firebase.appdistribution.gradle.firebaseAppDistribution
import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class FirebaseConventionPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    with(target) {
      // Runtime deps are always declared so src/release & src/preRelease compile;
      // the Gradle plugins (which fail without google-services.json) are conditional.
      dependencies {
        add("implementation", platform(libs.findLibrary("google-firebase-bom").get()))
        add("implementation", libs.findLibrary("google-firebase-analytics").get())
        add("implementation", libs.findLibrary("google-firebase-crashlytics").get())
      }

      val appDistributionDep = libs.findLibrary("google-firebase-appdistribution").get()
      configurations
        .matching { it.name == "alphaImplementation" || it.name == "betaImplementation" }
        .configureEach {
          dependencies.add(appDistributionDep.get())
        }

      val googleServicesFile = rootProject.file("app/android/google-services.json")
      if (!googleServicesFile.exists()) {
        logger.lifecycle(
          "Firebase plugins disabled: app/android/google-services.json not found. " +
            "APK will compile but Firebase runtime features will be inactive.",
        )
        return
      }

      with(pluginManager) {
        apply("com.google.gms.google-services")
        apply("com.google.firebase.crashlytics")
        apply("com.google.firebase.appdistribution")
      }

      extensions.configure<ApplicationExtension> {
        buildTypes.named("release") {
          configure<CrashlyticsExtension> {
            mappingFileUploadEnabled = true
          }
        }

        productFlavors.matching { it.name == "alpha" }.configureEach {
          firebaseAppDistribution {
            artifactType = "APK"
            groups = "internal,alpha-public"
          }
        }

        productFlavors.matching { it.name == "beta" }.configureEach {
          firebaseAppDistribution {
            artifactType = "APK"
            groups = "internal,external-public"
          }
        }
      }
    }
  }
}
