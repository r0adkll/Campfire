/**
 *
 * Please note:
 * This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * Do not edit this file manually.
 *
 */

@file:Suppress(
  "ArrayInDataClass",
  "EnumEntryName",
  "RemoveRedundantQualifierName",
  "UnusedImport",
)

package app.campfire.network.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 *
 *
 * @param id The ID of the notification.
 * @param libraryId The ID of the library. Applies to all libraries if `null`.
 * @param eventName
 * @param urls The Apprise URLs to use for the notification.
 * @param titleTemplate The template for the notification title.
 * @param bodyTemplate The template for the notification body.
 * @param enabled Whether the notification is enabled.
 * @param type
 * @param lastFiredAt The time (in ms since POSIX epoch) when the notification was last fired. Will be null if the notification has not fired.
 * @param lastAttemptFailed Whether the last notification attempt failed.
 * @param numConsecutiveFailedAttempts The number of consecutive times the notification has failed.
 * @param numTimesFired The number of times the notification has fired.
 * @param createdAt The time (in ms since POSIX epoch) when was created.
 */
@Serializable
data class Notification(

  /* The ID of the notification. */
  @SerialName(value = "id")
  val id: kotlin.String? = null,

  /* The ID of the library. Applies to all libraries if `null`. */
  @SerialName(value = "libraryId")
  val libraryId: String,

  @SerialName(value = "eventName")
  val eventName: app.campfire.network.models.NotificationEventName? = null,

  /* The Apprise URLs to use for the notification. */
  @SerialName(value = "urls")
  val urls: kotlin.collections.List<kotlin.String>? = null,

  /* The template for the notification title. */
  @SerialName(value = "titleTemplate")
  val titleTemplate: kotlin.String? = null,

  /* The template for the notification body. */
  @SerialName(value = "bodyTemplate")
  val bodyTemplate: kotlin.String? = null,

  /* Whether the notification is enabled. */
  @SerialName(value = "enabled")
  val enabled: kotlin.Boolean? = false,

  @SerialName(value = "type")
  val type: NotificationType? = NotificationType.Info,

  /* The time (in ms since POSIX epoch) when the notification was last fired. Will be null if the notification has not fired. */
  @SerialName(value = "lastFiredAt")
  val lastFiredAt: kotlin.Int? = null,

  /* Whether the last notification attempt failed. */
  @SerialName(value = "lastAttemptFailed")
  val lastAttemptFailed: kotlin.Boolean? = null,

  /* The number of consecutive times the notification has failed. */
  @SerialName(value = "numConsecutiveFailedAttempts")
  val numConsecutiveFailedAttempts: kotlin.Int? = 0,

  /* The number of times the notification has fired. */
  @SerialName(value = "numTimesFired")
  val numTimesFired: kotlin.Int? = 0,

  /* The time (in ms since POSIX epoch) when was created. */
  @SerialName(value = "createdAt")
  val createdAt: kotlin.Int? = null,

)
