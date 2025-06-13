import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
  id("app.campfire.kotlin.jvm")
  id("app.campfire.compose")
  alias(libs.plugins.ksp)
  alias(libs.plugins.metro)
  alias(libs.plugins.about.libraries)
}

metro {
  debug.set(false)
  transformProvidersToPrivate.set(false)
  reportsDestination.set(layout.buildDirectory.dir("metro/reports"))
}

kotlin {
  compilerOptions {
    freeCompilerArgs.add("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")
  }
}

dependencies {
  implementation(projects.app.common)
  implementation(compose.desktop.currentOs)

  implementation(libs.circuit.codegen.annotations)

  ksp(libs.circuit.codegen)
}

ksp {
  arg("circuit.codegen.mode", "metro")
}

compose.desktop {
  application {
    mainClass = "app.campfire.MainKt"

    nativeDistributions {
      targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
      packageName = "app.campfire"
      packageVersion = "1.0.0"
    }
  }
}

aboutLibraries {
  android.registerAndroidTasks = false
  export.prettyPrint = true
}
