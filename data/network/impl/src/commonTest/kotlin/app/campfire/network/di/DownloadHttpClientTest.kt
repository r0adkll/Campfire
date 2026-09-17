// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.di

import app.campfire.core.app.ApplicationInfo
import app.campfire.core.app.Flavor
import assertk.assertThat
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.pluginOrNull
import kotlin.test.Test

class DownloadHttpClientTest {

  private val client = createDownloadHttpClient(
    ApplicationInfo(
      packageName = "app.campfire",
      debugBuild = true,
      flavor = Flavor.Standard,
      versionName = "1.0",
      versionCode = 1,
      osName = "test",
      osVersion = "1",
    ),
  )

  @Test
  fun streamsWithoutAResponseCacheOrAuthOfItsOwn() {
    // HttpCache would read whole files into memory; a second Auth plugin would race the user
    // client's refresh of the single-use refresh token
    assertThat(client.pluginOrNull(HttpCache)).isNull()
    assertThat(client.pluginOrNull(Auth)).isNull()
    assertThat(client.pluginOrNull(HttpTimeout)).isNotNull()
  }
}
