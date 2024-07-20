import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
  id("app.campfire.kotlin.jvm")
  id("app.campfire.compose")
}

dependencies {
  implementation(projects.shared)
  implementation(compose.desktop.currentOs)
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
