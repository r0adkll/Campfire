package app.campfire.script.commands

import app.campfire.script.di.ScriptScope
import com.github.ajalt.clikt.command.CoreSuspendingCliktCommand
import com.github.ajalt.clikt.command.SuspendingCliktCommand
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import java.io.File

@ContributesIntoSet(
  scope = ScriptScope::class,
  binding = binding<CoreSuspendingCliktCommand>(),
)
@Inject
class CleanDesktopCommand : SuspendingCliktCommand(
  name = "clean-desktop",
) {

  override suspend fun run() {
    echo("Cleaning desktop user data…")

    // Configure Dirs
    val configDir = File(System.getProperty("user.home"), ".config")
    val appDir = File(configDir, "Campfire")

    echo("Cleaning $appDir")

    appDir.listFiles()?.forEach { file ->
      echo("Deleting ${file.name}")
      if (file.isDirectory) {
        file.deleteRecursively()
      } else {
        file.delete()
      }
    }

    echo("Desktop user data cleaned!")
  }
}
