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
  alias(libs.plugins.firebase.crashlytics) apply false
  alias(libs.plugins.firebase.appdistribution) apply false
  alias(libs.plugins.google.services) apply false
  alias(libs.plugins.kotlin.android) apply false
  alias(libs.plugins.kotlin.parcelize) apply false
  alias(libs.plugins.kotlin.serialization) apply false
  alias(libs.plugins.ksp) apply false
  alias(libs.plugins.spotless) apply false
  alias(libs.plugins.sqldelight) apply false
  alias(libs.plugins.about.libraries) apply false
  alias(libs.plugins.baselineprofile) apply false
  alias(libs.plugins.modulegraph)
}

tasks.register<Copy>("bootstrap") {
  from(file("scripts/pre-push"))
  into(file(".git/hooks"))
}

moduleGraphConfig {
  readmePath.set("$rootDir/docs/Architecture.md")
  heading.set("## Feature Graph Structure")
  nestingEnabled.set(true)
  rootModulesRegex.set(":features:.*")
  excludedModulesRegex.set(".*(common|core|infra|data|ui).*")
}
