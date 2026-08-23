// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

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
) {

  val userAgent: String
    get() = buildString {
      // Append application name + Flavor
      append("Campfire")
      append(
        when (flavor) {
          Flavor.Alpha -> " Alpha"
          else -> ""
        },
      )

      // Append application version
      append("/$versionName ")

      // Append OS information
      append("($osName $osVersion; Mobile)")
    }
}

enum class Flavor {
  Standard, Alpha, Foss
}
