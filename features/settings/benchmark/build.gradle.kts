// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

plugins {
  id("app.campfire.android.benchmark")
  id("app.campfire.kotlin.android")
}

dependencies {
  androidTestImplementation(projects.features.settings.impl)
  androidTestImplementation(libs.androidx.benchmark.junit4)
  androidTestImplementation(libs.androidx.datastore.preferences.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.kotlinx.coroutines.android)
}
