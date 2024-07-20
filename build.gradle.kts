plugins {
  id("app.campfire.root")

  alias(libs.plugins.kotlin.multiplatform) apply false
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.android.library) apply false
  alias(libs.plugins.android.lint) apply false
  alias(libs.plugins.android.test) apply false
  alias(libs.plugins.cacheFixPlugin) apply false
  alias(libs.plugins.compose.multiplatform) apply false
  alias(libs.plugins.compose.compiler) apply false
//    alias(libs.plugins.firebase.crashlytics) apply false
//    alias(libs.plugins.gms.googleServices) apply false
  alias(libs.plugins.kotlin.android) apply false
  alias(libs.plugins.kotlin.parcelize) apply false
  alias(libs.plugins.kotlin.serialization) apply false
  alias(libs.plugins.ksp) apply false
  alias(libs.plugins.spotless) apply false
  alias(libs.plugins.sqldelight) apply false
}

tasks.register<Copy>("bootstrap") {
  from(file("scripts/pre-push"))
  into(file(".git/hooks"))
}
