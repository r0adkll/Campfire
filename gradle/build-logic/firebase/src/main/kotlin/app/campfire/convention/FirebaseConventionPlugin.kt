// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.convention

import com.android.build.api.dsl.ApplicationExtension
import com.google.firebase.appdistribution.gradle.firebaseAppDistribution
import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import com.google.gms.googleservices.GoogleServicesTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

class FirebaseConventionPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    with(target) {
      // The Crashlytics runtime ships via :data:crashreporting:firebase, wired per flavor
      // in the app module, so nothing Firebase reaches the foss flavor. App Distribution is
      // upload-only: existing testers still get alpha/beta builds from it, but the in-app
      // updater is Drop's.
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

      // Debug builds never initialize Firebase, and their `.debug` application ids have no
      // client in google-services.json, which fails the task outright.
      tasks.withType<GoogleServicesTask>().configureEach {
        if (name.endsWith("DebugGoogleServices")) enabled = false
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
