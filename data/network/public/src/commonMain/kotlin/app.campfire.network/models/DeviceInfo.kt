package app.campfire.network.models

import kotlinx.serialization.Serializable

@Serializable
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
