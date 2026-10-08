// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.convention

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    with(target) {
      with(pluginManager) {
        apply("com.android.application")
        apply("org.gradle.android.cache-fix")
      }

      // Firebase (Crashlytics + App Distribution) is isolated in the :firebase build-logic
      // module so its proprietary Gradle plugins can be dropped from FOSS builds. Apply it
      // whenever that module is present — the same guard build-logic uses to include it — so
      // the alpha/beta App Distribution SDK that src/preRelease compiles against is wired even
      // without a google-services.json (fork PRs, local builds); the plugin itself skips the
      // Firebase Gradle plugins in that case. F-Droid scandeletes the module, so the id is
      // never requested there.
      if (rootProject.file("gradle/build-logic/firebase/build.gradle.kts").exists()) {
        pluginManager.apply("app.campfire.firebase")
      }

      configureAndroid(computeNamespace = false)
      configureLauncherTasks()

      // Add resource exclusions to just release builds
      androidComponents {
        onVariants(selector().withBuildType("release")) {
          it.packaging.resources.excludes.addAll(
            // Exclude AndroidX version files
            "META-INF/*.version",
            // Exclude consumer proguard files
            "META-INF/proguard/*",
            // Exclude the Firebase/Fabric/other random properties files
            "/*.properties",
            "fabric/*.properties",
            "META-INF/*.properties",
            // License files
            "LICENSE*",
            // Exclude Kotlin unused files
            "META-INF/**/previous-compilation-data.bin",
          )
        }
      }
    }
  }
}

private fun Project.androidComponents(action: ApplicationAndroidComponentsExtension.() -> Unit) =
  extensions.configure<ApplicationAndroidComponentsExtension>(action)
