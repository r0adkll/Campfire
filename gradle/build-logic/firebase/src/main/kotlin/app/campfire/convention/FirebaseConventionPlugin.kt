// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.convention

import com.android.build.api.dsl.ApplicationExtension
import com.google.firebase.appdistribution.gradle.firebaseAppDistribution
import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class FirebaseConventionPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    with(target) {
      // The Crashlytics runtime ships via :data:crashreporting:firebase, wired per flavor
      // in the app module, so nothing Firebase reaches the foss flavor. The only direct
      // dependency added here is the App Distribution SDK that powers the in-app
      // self-updater on the alpha channel (used by src/preRelease sources).
      val appDistributionDep = libs.findLibrary("google-firebase-appdistribution").get()
      configurations
        .matching { it.name == "alphaImplementation" }
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
      }
    }
  }
}
