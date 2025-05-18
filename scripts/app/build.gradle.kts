plugins {
  id("app.campfire.root")
  kotlin("jvm")
  application
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.metro)
}

application {
  mainClass.set("app.campfire.script.MainKt")
}

dependencies {
  implementation(libs.clikt)
  implementation(libs.mosaic)

  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.immutable)
}
