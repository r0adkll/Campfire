// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.convention

import org.gradle.api.Project

fun Project.configureKotlin() {
  // Configure Java to use our chosen language level. Kotlin will automatically pick this up
  configureJava()
}
