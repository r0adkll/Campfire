// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.ui.login

import app.campfire.core.model.NetworkSettings

/** The headers a Cloudflare Access service token is presented in. */
internal object CloudflareAccessHeaders {
  const val CLIENT_ID = "CF-Access-Client-Id"
  const val CLIENT_SECRET = "CF-Access-Client-Secret"
}

/** The service token's Client ID in these settings, if any. Header names are case-insensitive. */
internal val NetworkSettings?.cloudflareClientId: String?
  get() = headerValue(CloudflareAccessHeaders.CLIENT_ID)

/** The service token's Client Secret in these settings, if any. */
internal val NetworkSettings?.cloudflareClientSecret: String?
  get() = headerValue(CloudflareAccessHeaders.CLIENT_SECRET)

internal val NetworkSettings?.hasCloudflareServiceToken: Boolean
  get() = cloudflareClientId != null

/** These settings with the service token replaced by [clientId] and [clientSecret]. */
internal fun NetworkSettings?.withCloudflareServiceToken(
  clientId: String,
  clientSecret: String,
): NetworkSettings {
  val otherHeaders = this?.extraHeaders.orEmpty().filterKeys { name ->
    !name.equals(CloudflareAccessHeaders.CLIENT_ID, ignoreCase = true) &&
      !name.equals(CloudflareAccessHeaders.CLIENT_SECRET, ignoreCase = true)
  }
  return NetworkSettings(
    extraHeaders = otherHeaders + mapOf(
      CloudflareAccessHeaders.CLIENT_ID to clientId.trim(),
      CloudflareAccessHeaders.CLIENT_SECRET to clientSecret.trim(),
    ),
  )
}

private fun NetworkSettings?.headerValue(name: String): String? =
  this?.extraHeaders?.entries?.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value
