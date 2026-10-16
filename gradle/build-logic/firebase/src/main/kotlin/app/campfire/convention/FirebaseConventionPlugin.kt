// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.convention

import com.android.build.api.dsl.ApplicationExtension
import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class FirebaseConventionPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    with(target) {
      // The Crashlytics runtime ships via :data:crashreporting:firebase, wired per flavor
      // in the app module, so nothing Firebase reaches the foss flavor.
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
      }

      extensions.configure<ApplicationExtension> {
        buildTypes.named("release") {
          configure<CrashlyticsExtension> {
            mappingFileUploadEnabled = true
          }
        }
      }
    }
  }
}
