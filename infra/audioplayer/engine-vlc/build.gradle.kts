// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

plugins {
  id("app.campfire.kotlin.jvm")
  alias(libs.plugins.ksp)
}

dependencies {
  implementation(projects.infra.audioplayer.impl)
  implementation(projects.core)

  implementation(libs.vlcj)
  implementation(libs.kotlinx.coroutines.core)

  implementation(libs.kimchi.annotations)
  implementation(libs.kotlininject.runtime)
  ksp(libs.kotlininject.ksp)
  ksp(libs.kimchi.compiler)
}
