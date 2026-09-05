// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

plugins {
  id("app.campfire.kotlin.jvm")
}

dependencies {
  testImplementation(projects.infra.audioplayer.impl)
  testImplementation(projects.infra.audioplayer.engineVlc)
  testImplementation(projects.infra.audioplayer.engineFfmpeg)
  testImplementation(projects.infra.audioplayer.test)
  testImplementation(projects.features.settings.test)
  testImplementation(projects.core)

  testImplementation(libs.bundles.test.common)
  testImplementation(libs.kotlinx.coroutines.core)
  testImplementation(libs.kotlinx.serialization.json)
}
