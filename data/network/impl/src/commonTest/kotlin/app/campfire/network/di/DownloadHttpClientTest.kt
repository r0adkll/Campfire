// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.di

import app.campfire.core.app.ApplicationInfo
import app.campfire.core.app.Flavor
import app.campfire.network.plugins.RedirectHeaderGuard
import app.campfire.network.reachability.FakeWorkOffline
import app.campfire.network.reachability.WorkingOfflineException
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.pluginOrNull
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class DownloadHttpClientTest {

  private val applicationInfo = ApplicationInfo(
    packageName = "app.campfire",
    debugBuild = true,
    flavor = Flavor.Standard,
    versionName = "1.0",
    versionCode = 1,
    osName = "test",
    osVersion = "1",
  )
  private val workOffline = FakeWorkOffline()

  private var calls = 0
  private val engine = MockEngine {
    calls++
    respondOk("file-bytes")
  }

  private val client = HttpClient(engine) {
    configureDownloadHttpClient(applicationInfo, workOffline)
  }

  @Test
  fun streamsWithoutAResponseCacheOrAuthOfItsOwn() {
    // HttpCache would read whole files into memory; a second Auth plugin would race the user
    // client's refresh of the single-use refresh token
    assertThat(client.pluginOrNull(HttpCache)).isNull()
    assertThat(client.pluginOrNull(Auth)).isNull()
    assertThat(client.pluginOrNull(HttpTimeout)).isNotNull()
    assertThat(client.pluginOrNull(RedirectHeaderGuard)).isNotNull()
  }

  @Test
  fun makesNoRequestsWhileWorkingOffline() = runTest {
    workOffline.offline = true

    assertFailure { client.get("https://abs.example.com/api/items/1/ebook") }
      .isInstanceOf<WorkingOfflineException>()
    assertThat(calls).isEqualTo(0)
  }

  @Test
  fun downloadsAgainOnceBackOnline() = runTest {
    workOffline.offline = true
    workOffline.offline = false

    assertThat(client.get("https://abs.example.com/api/items/1/ebook").bodyAsText()).isEqualTo("file-bytes")
    assertThat(calls).isEqualTo(1)
  }
}
