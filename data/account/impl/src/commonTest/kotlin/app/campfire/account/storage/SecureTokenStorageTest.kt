// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.account.storage

import app.campfire.account.api.AbsToken
import app.campfire.common.test.coroutines.asTestDispatcherProvider
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class SecureTokenStorageTest {

  @Test
  fun `round trips a token`() = runTest {
    val storage = SecureTokenStorage(MapSettings(), asTestDispatcherProvider())

    storage.put(USER, AbsToken("access", "refresh"))

    assertThat(storage.get(USER)).isEqualTo(AbsToken("access", "refresh"))
  }

  @Test
  fun `a token without a refresh token drops the old one`() = runTest {
    val storage = SecureTokenStorage(MapSettings(), asTestDispatcherProvider())

    storage.put(USER, AbsToken("access", "refresh"))
    storage.put(USER, AbsToken("rotated", null))

    assertThat(storage.get(USER)).isEqualTo(AbsToken("rotated", null))
  }

  @Test
  fun `has reports whether a user has a stored token`() = runTest {
    val storage = SecureTokenStorage(MapSettings(), asTestDispatcherProvider())
    assertThat(storage.has(USER)).isFalse()

    storage.put(USER, AbsToken("access", "refresh"))
    assertThat(storage.has(USER)).isTrue()
    assertThat(storage.has(OTHER_USER)).isFalse()

    storage.remove(USER)
    assertThat(storage.has(USER)).isFalse()
    assertThat(storage.get(USER)).isNull()
  }

  private companion object {
    const val USER = "user-1"
    const val OTHER_USER = "user-2"
  }
}
