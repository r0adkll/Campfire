// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.account.blockstore

import android.app.Application
import app.campfire.account.api.BackedUpAccount
import app.campfire.account.api.TokenBackup
import app.campfire.account.backup.NoOpTokenBackup
import app.campfire.core.di.AppScope
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.core.model.UserId
import com.google.android.gms.auth.blockstore.Blockstore
import com.google.android.gms.auth.blockstore.BlockstoreClient
import com.google.android.gms.auth.blockstore.DeleteBytesRequest
import com.google.android.gms.auth.blockstore.RetrieveBytesRequest
import com.google.android.gms.auth.blockstore.StoreBytesData
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.tasks.await

/**
 * Keeps sign-ins in Google Play services' Block Store, which survives the app being reinstalled
 * while the user's Google backup is on, and goes to a new device with it when the backup is
 * end-to-end encrypted. One key per account.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, replaces = [NoOpTokenBackup::class])
@Inject
class BlockStoreTokenBackup(
  private val application: Application,
) : TokenBackup {

  private val client: BlockstoreClient by lazy { Blockstore.getClient(application) }

  override val isAvailable: Boolean = true

  override suspend fun put(account: BackedUpAccount) = guard("keep an account") {
    val bytes = encodeKeptAccount(account)
    if (bytes == null) {
      bark(LogPriority.WARN) { "Account is too large to keep" }
      return@guard
    }

    val data = StoreBytesData.Builder()
      .setKey(keyFor(account.userId))
      .setBytes(bytes)
      // Only to the cloud when it's end-to-end encrypted. Writing with this off deletes the cloud
      // copy, so it's decided on every write rather than once.
      .setShouldBackupToCloud(client.isEndToEndEncryptionAvailable().await())
      .build()
    client.storeBytes(data).await()
  }

  override suspend fun remove(userId: UserId) = guard("forget an account") {
    delete(listOf(keyFor(userId)))
  }

  override suspend fun clear() = guard("forget every account") {
    delete(retrieveAll().keys.toList())
  }

  override suspend fun getAll(): List<BackedUpAccount> =
    guard("read kept accounts", default = emptyList()) {
      retrieveAll().values.mapNotNull { decodeKeptAccount(it) }
    }

  private suspend fun retrieveAll(): Map<String, ByteArray> {
    val request = RetrieveBytesRequest.Builder()
      .setRetrieveAll(true)
      .build()
    return client.retrieveBytes(request).await()
      .blockstoreDataMap
      .filterKeys { it.startsWith(KEY_PREFIX) }
      .mapValues { (_, data) -> data.bytes }
  }

  private suspend fun delete(keys: List<String>) {
    if (keys.isEmpty()) return
    val request = DeleteBytesRequest.Builder()
      .setKeys(keys)
      .build()
    client.deleteBytes(request).await()
  }

  /**
   * Block Store failing (no Play services, a full store, …) must never break signing in
   */
  private suspend fun <T> guard(action: String, default: T, block: suspend () -> T): T = try {
    block()
  } catch (e: CancellationException) {
    throw e
  } catch (e: Exception) {
    bark(LogPriority.WARN, throwable = e) { "Block Store couldn't $action" }
    default
  }

  private suspend fun guard(action: String, block: suspend () -> Unit) = guard(action, Unit, block)
}
