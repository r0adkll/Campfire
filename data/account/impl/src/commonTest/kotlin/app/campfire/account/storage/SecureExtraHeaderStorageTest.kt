// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.account.storage

import app.campfire.common.test.coroutines.asTestDispatcherProvider
import app.campfire.core.logging.LogRedaction
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class SecureExtraHeaderStorageTest {

  @Test
  fun `observers see writes made through the same storage`() = runTest {
    val storage = SecureExtraHeaderStorage(MapSettings(), asTestDispatcherProvider())

    storage.observe(USER).test {
      assertThat(awaitItem()).isEmpty()

      storage.put(USER, mapOf("X-Token" to "one"))
      assertThat(awaitItem()).isEqualTo(mapOf("X-Token" to "one"))

      storage.put(USER, mapOf("X-Token" to "two"))
      assertThat(awaitItem()).isEqualTo(mapOf("X-Token" to "two"))

      storage.remove(USER)
      assertThat(awaitItem()).isEmpty()
    }
  }

  @Test
  fun `a write for another user doesn't re-emit`() = runTest {
    val storage = SecureExtraHeaderStorage(MapSettings(), asTestDispatcherProvider())

    storage.observe(USER).test {
      assertThat(awaitItem()).isEmpty()

      storage.put("someone-else", mapOf("X-Token" to "one"))
      expectNoEvents()
    }
  }

  @Test
  fun `stored and loaded header values are redacted from logs`() = runTest {
    LogRedaction.enabled = true
    LogRedaction.clear()
    try {
      val settings = MapSettings()
      SecureExtraHeaderStorage(settings, asTestDispatcherProvider())
        .put(USER, mapOf("CF-Access-Client-Secret" to "written-secret-value"))
      assertThat(LogRedaction.redact("sent written-secret-value")).isEqualTo("sent <secret>")

      // A fresh process only sees the values once it reads them back.
      LogRedaction.clear()
      SecureExtraHeaderStorage(settings, asTestDispatcherProvider()).get(USER)
      assertThat(LogRedaction.redact("sent written-secret-value")).isEqualTo("sent <secret>")
    } finally {
      LogRedaction.clear()
    }
  }

  private companion object {
    const val USER = "user-id"
  }
}
