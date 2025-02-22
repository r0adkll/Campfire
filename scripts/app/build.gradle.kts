plugins {
  id("app.campfire.root")
  kotlin("jvm")
  application
  alias(libs.plugins.ksp)
  alias(libs.plugins.compose.compiler)
}

application {
  mainClass.set("app.campfire.script.MainKt")
}

dependencies {
  implementation(libs.clikt)
  implementation(libs.mosaic)

  implementation(libs.kimchi.annotations)
  implementation(libs.kotlininject.runtime)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.immutable)

  ksp(libs.kimchi.compiler)
  ksp(libs.kotlininject.ksp)
}
