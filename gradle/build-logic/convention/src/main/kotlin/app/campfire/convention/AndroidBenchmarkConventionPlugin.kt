// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.convention

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * A Jetpack Microbenchmark module: an Android library whose instrumented tests are the benchmarks. They run
 * against a non-debuggable release build, as the benchmark library requires for meaningful timings.
 */
class AndroidBenchmarkConventionPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    with(target) {
      with(pluginManager) {
        apply("com.android.library")
        apply("androidx.benchmark")
        apply("org.gradle.android.cache-fix")
      }

      configureAndroid()

      extensions.configure<LibraryExtension> {
        defaultConfig.testInstrumentationRunner = "androidx.benchmark.junit4.AndroidBenchmarkRunner"
        testBuildType = "release"
        buildTypes.getByName("release").isDefault = true
      }
    }
  }
}
