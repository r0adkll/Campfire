package app.campfire.baselineprofile

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.uiAutomator
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMetricApi::class)
@RunWith(AndroidJUnit4::class)
@LargeTest
class StartupBenchmarks {

  @get:Rule
  val rule = MacrobenchmarkRule()

  // No ahead-of-time (AOT) compilation at all. Represents performance of a
  // fresh install on a user's device if you don't enable Baseline Profiles—
  // generally the worst case performance.
  @Test
  fun startupNoCompilation() = startup(CompilationMode.None())

  // Partial pre-compilation with Baseline Profiles. Represents performance of
  // a fresh install on a user's device.
  @Test
  fun startupPartialWithBaselineProfiles() =
    startup(CompilationMode.Partial(baselineProfileMode = BaselineProfileMode.Require))

  // Partial pre-compilation with some just-in-time (JIT) compilation.
  // Represents performance after some app usage.
  @Test
  fun startupPartialCompilation() = startup(
    CompilationMode.Partial(
      baselineProfileMode = BaselineProfileMode.Disable,
      warmupIterations = 3,
    ),
  )

  // Full pre-compilation. Generally not representative of real user
  // experience, but can yield more stable performance metrics by removing
  // noise from JIT compilation within benchmark runs.
  @Test
  fun startupFullCompilation() = startup(CompilationMode.Full())

  private fun startup(compilationMode: CompilationMode) {
    // The application id for the running build variant is read from the instrumentation arguments.
    rule.measureRepeated(
      packageName = InstrumentationRegistry.getArguments().getString("targetAppId")
        ?: throw Exception("targetAppId not passed as instrumentation runner arg"),
      metrics = listOf(StartupTimingMetric()),
      compilationMode = compilationMode,
      startupMode = StartupMode.COLD,
      iterations = 10,
      setupBlock = {
        pressHome()
      },
      measureBlock = {
        uiAutomator {
          startApp(packageName = packageName)
          waitForAppToBeVisible(packageName)
          handleSignIn()
        }
      },
    )
  }
}
