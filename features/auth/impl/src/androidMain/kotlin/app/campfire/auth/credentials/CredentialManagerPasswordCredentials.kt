// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.credentials

import android.os.Build
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPasswordOption
import androidx.credentials.PasswordCredential
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialException
import app.campfire.auth.api.PasswordCredentials
import app.campfire.core.di.AppScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Saves and finds passwords through Credential Manager, in whatever password manager the user
 * picked (Google Password Manager, Bitwarden, …).
 *
 * Only on Android 14+, where Credential Manager is part of the platform. Older versions would need
 * the Play services provider, which the F-Droid build can't ship; they keep plain autofill.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, replaces = [NoOpPasswordCredentials::class])
@Inject
class CredentialManagerPasswordCredentials(
  private val activityHolder: CredentialActivityHolder,
  @ForScope(AppScope::class) private val applicationScope: CoroutineScope,
) : PasswordCredentials {

  private val isSupported: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

  override fun offerToSave(serverUrl: String, userName: String, password: String) {
    if (!isSupported) return
    val activity = activityHolder.activity ?: return

    // Signing in swaps the whole UI out from under the caller, so the prompt can't live in its scope
    applicationScope.launch {
      try {
        CredentialManager.create(activity).createCredential(
          context = activity,
          request = CreatePasswordRequest(
            id = passwordCredentialId(serverUrl, userName),
            password = password,
          ),
        )
      } catch (e: CreateCredentialException) {
        bark(LogPriority.INFO, throwable = e) { "Password wasn't saved" }
      }
    }
  }

  override suspend fun find(serverUrl: String, userName: String): String? {
    if (!isSupported) return null
    val activity = activityHolder.activity ?: return null

    return try {
      val response = CredentialManager.create(activity).getCredential(
        context = activity,
        request = GetCredentialRequest(
          listOf(GetPasswordOption(allowedUserIds = setOf(passwordCredentialId(serverUrl, userName)))),
        ),
      )
      (response.credential as? PasswordCredential)?.password
    } catch (e: GetCredentialException) {
      // Includes having no saved password and the user dismissing the sheet
      bark(LogPriority.INFO, throwable = e) { "No saved password used" }
      null
    }
  }
}
