// Copyright 2023, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: Apache-2.0

package app.campfire.convention

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.configure

fun Project.configureJava() {
  java {
    toolchain {
      languageVersion.set(JavaLanguageVersion.of(21))
    }
  }
}

private fun Project.java(action: JavaPluginExtension.() -> Unit) = extensions.configure<JavaPluginExtension>(action)
