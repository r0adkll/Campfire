package app.campfire.script.commands

import app.campfire.script.CampfireConfig
import app.campfire.script.di.ScriptScope
import app.campfire.script.gradle.gw
import com.github.ajalt.clikt.command.CoreSuspendingCliktCommand
import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.requireObject
import com.r0adkll.kimchi.annotations.ContributesMultibinding
import me.tatarka.inject.annotations.Inject

@ContributesMultibinding(
  scope = ScriptScope::class,
  boundType = CoreSuspendingCliktCommand::class,
)
@Inject
class SpotlessApplyCommand : SuspendingCliktCommand(
  name = "spotless",
) {

  private val config by requireObject<CampfireConfig>()

  override suspend fun run() {
    gw("spotlessApply", quiet = !config.verbose) {
      onInput { line ->
        if (config.verbose) echo(line)
      }
      onError { echo(it) }
    }.onFailure {
      echoFormattedHelp(
        CliktError(
          message = "Running spotless on the main project failed",
          statusCode = code,
        ),
      )
    }.onSuccess {
      echo("Spotless applied to main project!")
    }

    gw("-p", "gradle/build-logic", "spotlessApply", quiet = !config.verbose) {
      onInput { line ->
        if (config.verbose) echo(line)
      }
      onError { echo(it) }
    }.onFailure {
      echoFormattedHelp(
        CliktError(
          message = "Running spotless on the main project failed",
          statusCode = code,
        ),
      )
    }.onSuccess {
      echo("Spotless applied to build logic!")
    }
  }
}
