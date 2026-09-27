// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.baselineprofile

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.MemoryUsageMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.onElement
import androidx.test.uiautomator.uiAutomator
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Measures the runtime cost of dependency injection during a signed-in cold start: the time spent
 * building each dependency graph (see `DiTraceSections` in `:infra:tracing`) alongside overall
 * startup.
 *
 * Unlike [StartupBenchmarks], this compiles the app fully ahead of time instead of against the
 * baseline profile, so a before/after comparison of two DI frameworks isn't skewed by a profile
 * that was generated for only one of them.
 *
 * Driven by `tools/benchmark/run.py runtime`, which runs it on a connected device and archives
 * the results for comparison. By hand:
 * ```
 * ./gradlew :app:baselineprofile:connectedBenchmarkReleaseAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.class=app.campfire.baselineprofile.DiStartupBenchmarks
 * ```
 */
@OptIn(ExperimentalMetricApi::class)
@RunWith(AndroidJUnit4::class)
@LargeTest
class DiStartupBenchmarks {

  @get:Rule
  val rule = MacrobenchmarkRule()

  /**
   * Cold start of a signed-in user to Home content, a user's typical launch. Signs in once with the
   * login form's prefilled test credentials (`campfire_server_url` / `campfire_username` /
   * `campfire_password` in ~/.gradle/gradle.properties), then measures each launch from a cold
   * process until Home shows library items.
   */
  @Test
  fun coldStartup() {
    var signedIn = false
    rule.measureRepeated(
      packageName = InstrumentationRegistry.getArguments().getString("targetAppId")
        ?: throw Exception("targetAppId not passed as instrumentation runner arg"),
      metrics = listOf(
        StartupTimingMetric(),
        MemoryUsageMetric(MemoryUsageMetric.Mode.Last),
        TraceSectionMetric(APP_GRAPH, TraceSectionMetric.Mode.First),
        TraceSectionMetric(ACTIVITY_GRAPH, TraceSectionMetric.Mode.First),
        TraceSectionMetric(USER_GRAPH, TraceSectionMetric.Mode.First),
      ),
      compilationMode = CompilationMode.Full(),
      startupMode = StartupMode.COLD,
      iterations = InstrumentationRegistry.getArguments().getString("iterations")?.toInt() ?: 20,
      setupBlock = {
        if (!signedIn) {
          // A notification shade left open on a personal device hides the login screen.
          device.executeShellCommand("cmd statusbar collapse")
          uiAutomator {
            startApp(packageName = packageName)
            handleSignIn()
            onElement(timeoutMs = HOME_TIMEOUT_MS) { contentDescription == "HomeLibraryItem" }
          }
          killProcess()
          signedIn = true
        }
        pressHome()
      },
      measureBlock = {
        uiAutomator {
          startApp(packageName = packageName)
          onElement(timeoutMs = HOME_TIMEOUT_MS) { contentDescription == "HomeLibraryItem" }
        }
      },
    )
  }

  private companion object {
    // Mirrors app.campfire.tracing.DiTraceSections; this test module can't depend on the app.
    const val APP_GRAPH = "di:createAppGraph"
    const val ACTIVITY_GRAPH = "di:createActivityGraph"
    const val USER_GRAPH = "di:createUserGraph"

    const val HOME_TIMEOUT_MS = 30_000L
  }
}
