// Copyright 2023, Google LLC, Christopher Banes and the Tivi project contributors
// SPDX-License-Identifier: Apache-2.0

package app.campfire.core.app

data class ApplicationInfo(
  val packageName: String,
  val debugBuild: Boolean,
  val flavor: Flavor,
  val versionName: String,
  val versionCode: Int,
  val osName: String,
  val osVersion: String,

  // Android Related fields
  val manufacturer: String? = null,
  val model: String? = null,
  val sdkVersion: Int? = null,
)

enum class Flavor {
  Standard, Beta, Alpha
}
