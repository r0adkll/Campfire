// Copyright 2023, Google LLC, Christopher Banes and the Tivi project contributors
// SPDX-License-Identifier: Apache-2.0

package app.campfire.core.app

data class ApplicationInfo(
  val packageName: String,
  val debugBuild: Boolean,
  val flavor: app.campfire.core.app.Flavor,
  val versionName: String,
  val versionCode: Int,
)

enum class Flavor {
  Standard,
}
