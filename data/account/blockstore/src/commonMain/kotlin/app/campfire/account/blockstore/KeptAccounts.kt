// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.account.blockstore

import app.campfire.account.api.AbsToken
import app.campfire.account.api.BackedUpAccount
import app.campfire.core.model.UserId
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Block Store keeps at most 4 KB per key
 */
internal const val MAX_ENTRY_BYTES = 4 * 1024

internal const val KEY_PREFIX = "campfire.account."

internal fun keyFor(userId: UserId): String = "$KEY_PREFIX$userId"

/**
 * The bytes to keep for [account], or null when they don't fit. Extra headers can be large (e.g.
 * an access gateway's token), so they're dropped first: the account can still be signed back into
 * where the server doesn't need them, and the user can add them again in settings.
 */
internal fun encodeKeptAccount(account: BackedUpAccount): ByteArray? {
  val kept = KeptAccount(account)
  return kept.encode().takeIf { it.size <= MAX_ENTRY_BYTES }
    ?: kept.copy(extraHeaders = emptyMap()).encode().takeIf { it.size <= MAX_ENTRY_BYTES }
}

/**
 * The account in [bytes], or null when they can't be read, e.g. written by a future version
 */
internal fun decodeKeptAccount(bytes: ByteArray): BackedUpAccount? =
  runCatching { json.decodeFromString<KeptAccount>(bytes.decodeToString()) }
    .getOrNull()
    ?.asBackedUpAccount()

@Serializable
internal data class KeptAccount(
  val serverUrl: String,
  val serverName: String,
  val userId: UserId,
  val accessToken: String,
  val refreshToken: String?,
  val extraHeaders: Map<String, String>,
) {

  constructor(account: BackedUpAccount) : this(
    serverUrl = account.serverUrl,
    serverName = account.serverName,
    userId = account.userId,
    accessToken = account.token.accessToken,
    refreshToken = account.token.refreshToken,
    extraHeaders = account.extraHeaders,
  )

  fun encode(): ByteArray = json.encodeToString(this).encodeToByteArray()

  fun asBackedUpAccount() = BackedUpAccount(
    serverUrl = serverUrl,
    serverName = serverName,
    userId = userId,
    token = AbsToken(accessToken, refreshToken),
    extraHeaders = extraHeaders,
  )
}

private val json = Json { ignoreUnknownKeys = true }
