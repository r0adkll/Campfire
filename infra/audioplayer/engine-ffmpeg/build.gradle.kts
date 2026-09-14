// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

plugins {
  id("app.campfire.kotlin.jvm")
  alias(libs.plugins.ksp)
}

dependencies {
  implementation(projects.infra.audioplayer.impl)
  implementation(projects.core)

  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.bytedeco.ffmpeg)
  implementation(libs.bytedeco.javacpp)

  implementation(libs.kimchi.annotations)
  implementation(libs.kotlininject.runtime)
  ksp(libs.kotlininject.ksp)
  ksp(libs.kimchi.compiler)

  testImplementation(libs.bundles.test.common)
  testRuntimeOnly(variantOf(libs.bytedeco.ffmpeg) { classifier(javacppPlatform) })
  testRuntimeOnly(variantOf(libs.bytedeco.javacpp) { classifier(javacppPlatform) })
}

/**
 * The JavaCPP classifier the tests load natives from: `campfire_javacpp_platform` when set,
 * otherwise the machine running the build. Only tests need a classifier resolved here — the
 * shipped natives are declared per machine in `:app:desktop`, so that one Conveyor run can package
 * every platform without the build host's natives leaking into all of them.
 */
val javacppPlatform: String
  get() {
    providers.gradleProperty("campfire_javacpp_platform").orNull?.let { return it }
    val os = System.getProperty("os.name").lowercase()
    val arch = System.getProperty("os.arch").lowercase()
    val osName = when {
      os.contains("mac") -> "macosx"
      os.contains("win") -> "windows"
      else -> "linux"
    }
    val archName = when {
      arch == "aarch64" || arch == "arm64" -> "arm64"
      else -> "x86_64"
    }
    return "$osName-$archName"
  }
