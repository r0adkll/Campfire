import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
  id("app.campfire.kotlin.jvm")
  id("app.campfire.compose")
  alias(libs.plugins.ksp)
}

kotlin {
  compilerOptions {
    freeCompilerArgs.add("-Xopt-in=androidx.compose.material3.ExperimentalMaterial3Api")
  }
}

dependencies {
  implementation(projects.shared)
  implementation(compose.desktop.currentOs)

  implementation(libs.kimchi.annotations)
  implementation(libs.kotlininject.runtime)

  ksp(libs.kotlininject.ksp)
  ksp(libs.kimchi.compiler)
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
