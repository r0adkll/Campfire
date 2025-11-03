package app.campfire.script.commands

import app.campfire.script.di.ScriptScope
import app.campfire.script.util.runtime
import app.campfire.script.util.stdin
import app.campfire.script.util.stdout
import com.github.ajalt.clikt.command.CoreSuspendingCliktCommand
import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.r0adkll.kimchi.annotations.ContributesMultibinding
import java.io.File
import java.io.IOException
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import me.tatarka.inject.annotations.Inject

@OptIn(ExperimentalTime::class)
@ContributesMultibinding(
  scope = ScriptScope::class,
  boundType = CoreSuspendingCliktCommand::class,
)
@Inject
class PerfettoCommand : SuspendingCliktCommand(
  name = "perfetto",
) {

  val packageName by option("-p", "--package", help = "The package name of the app to run against")
    .default("app.campfire.android")

  val duration: Int by option("-t", "--time", help = "The duration of the trace in seconds")
    .int().default(10)

  val buffer: Int by option("-b", "--buffer", help = "The size of the buffer in MB")
    .int().default(64)

  override suspend fun run() {
    // Enable tracing on device
    enableTracing(packageName)

    // Build config
    // TODO: Support more perfetto customizations
    val config = """
      buffers: {
          size_kb: ${buffer * 1024}
          fill_policy: RING_BUFFER
      }
      data_sources: {
          config {
              name: "track_event"
          }
      }
      duration_ms: ${duration * 1000}
      flush_period_ms: 30000
      incremental_state_config {
          clear_period_ms: 5000
      }
    """.trimIndent()

    // Start tracing
    try {
      startTrace(config)
      echo("Trace complete! Pulling from device")

      // Now complete, pull the trace
      val outputDir = File(".", "traces")
      val traceFileName = "${packageName}_${Clock.System.now()}.pftrace"
        .replace(":", "_")
        .replace(".", "_")
      val outputTraceFile = File(outputDir, traceFileName)
      outputDir.mkdirs()
      runtime("adb", "pull", "/data/misc/perfetto-traces/trace.pftrace", outputTraceFile.absolutePath) {
        process.stdout.bufferedReader().readText()
      }

      echo("Trace captured @ ${outputTraceFile.absolutePath}")
      // TODO: Support directly uploading this tracefile in perfetto ui
    } catch (e: IOException) {
      echo("Unable to complete perfetto trace", err = true)
      echo(e.stackTraceToString(), err = true)
    }
  }

  private suspend fun enableTracing(packageName: String) {
    echo("Enabling tracing for $packageName")
    runtime(
      "adb",
      "shell",
      "am",
      "broadcast",
      "-a",
      "androidx.tracing.perfetto.action.ENABLE_TRACING",
      "$packageName/androidx.tracing.perfetto.TracingReceiver",
    ) { process.stdout.bufferedReader().readText() }
  }

  private suspend fun startTrace(
    config: String,
  ) {
    echo("Config:")
    echo(config)
    echo("Capturing trace for 10s…")
    runtime(
      "adb",
      "shell",
      "perfetto",
      "-c",
      "-",
      "--txt",
      "-o",
      "/data/misc/perfetto-traces/trace.pftrace",
    ) {
      process.stdin.writer().use {
        it.write(config)
      }
      process.stdout.bufferedReader().readText()
    }
  }
}
