// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.model

data class DeviceInfo(
  val id: String,
  val userId: String,
  val deviceId: String,
  val ipAddress: String? = null,
  val browserName: String? = null,
  val browserVersion: String? = null,
  val osName: String? = null,
  val osVersion: String? = null,
  val deviceType: String? = null,
  val manufacturer: String? = null,
  val model: String? = null,
  val sdkVersion: Int? = null,
  val clientName: String,
  val clientVersion: String,
)
