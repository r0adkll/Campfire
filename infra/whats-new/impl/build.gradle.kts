import app.campfire.convention.addKspDependencyForCommon
import java.io.ByteArrayOutputStream

plugins {
  id("app.campfire.android.library")
  id("app.campfire.multiplatform")
  id("app.campfire.compose")
  alias(libs.plugins.ksp)
  alias(libs.plugins.kotlin.serialization)
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        api(projects.infra.whatsNew.api)

        implementation(projects.core)
        implementation(projects.features.settings.api)

        implementation(libs.kotlinx.serialization.json)

        implementation(compose.components.resources)
      }
    }
  }
}

addKspDependencyForCommon(libs.kimchi.compiler)

val generatedResources = layout.buildDirectory.dir("generated/composeResources")

abstract class GenerateChangelogTask @Inject constructor(
  private val execOperations: ExecOperations,
) : DefaultTask() {

  @get:InputFile
  abstract val changelogFile: RegularFileProperty

  @get:Input
  abstract val projectRootDir: Property<String>

  @get:OutputFile
  abstract val outputFile: RegularFileProperty

  @TaskAction
  fun generate() {
    val output = outputFile.get().asFile
    output.parentFile.mkdirs()

    val stdout = ByteArrayOutputStream()
    execOperations.exec {
      commandLine("./campfire", "-v", "changelog", "-aj")
      workingDir = File(projectRootDir.get())
      standardOutput = stdout
    }

    output.writeText(stdout.toString(Charsets.UTF_8))
  }
}

val generateChangelog by tasks.registering(GenerateChangelogTask::class) {
  changelogFile.set(rootProject.file("CHANGELOG.md"))
  projectRootDir.set(rootProject.rootDir.absolutePath)
  outputFile.set(generatedResources.map { it.file("files/changelog.json") })

  outputs.cacheIf { true }
}

compose.resources {
  customDirectory(
    sourceSetName = "commonMain",
    directoryProvider = generatedResources,
  )
}

tasks.matching {
  it.name.startsWith("generateComposeResClass") ||
    it.name.startsWith("copyNonXmlValueResourcesFor") ||
    it.name.startsWith("prepareComposeResourcesTaskFor") ||
    it.name.startsWith("generateResourceAccessorsFor")
}.configureEach {
  dependsOn(generateChangelog)
}
