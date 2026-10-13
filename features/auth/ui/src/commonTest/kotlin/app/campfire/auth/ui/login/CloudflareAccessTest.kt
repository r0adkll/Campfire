// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.ui.login

import app.campfire.core.model.NetworkSettings
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.test.Test

class CloudflareAccessTest {

  @Test
  fun `adding a service token keeps other headers`() {
    val settings = NetworkSettings(mapOf("X-Proxy" to "on"))
      .withCloudflareServiceToken(" id.access ", "secret ")

    assertThat(settings.extraHeaders).isEqualTo(
      mapOf(
        "X-Proxy" to "on",
        "CF-Access-Client-Id" to "id.access",
        "CF-Access-Client-Secret" to "secret",
      ),
    )
  }

  @Test
  fun `changing the service token replaces headers of any case`() {
    val settings = NetworkSettings(
      mapOf("cf-access-client-id" to "old.access", "CF-ACCESS-CLIENT-SECRET" to "old"),
    ).withCloudflareServiceToken("new.access", "new")

    assertThat(settings.extraHeaders).isEqualTo(
      mapOf("CF-Access-Client-Id" to "new.access", "CF-Access-Client-Secret" to "new"),
    )
  }

  @Test
  fun `the token is read case-insensitively`() {
    val settings = NetworkSettings(mapOf("cf-access-client-id" to "id.access", "cf-access-client-secret" to "s"))

    assertThat(settings.hasCloudflareServiceToken).isTrue()
    assertThat(settings.cloudflareClientId).isEqualTo("id.access")
    assertThat(settings.cloudflareClientSecret).isEqualTo("s")
  }

  @Test
  fun `no settings means no token`() {
    val settings: NetworkSettings? = null

    assertThat(settings.hasCloudflareServiceToken).isFalse()
    assertThat(settings.cloudflareClientSecret).isNull()
  }
}
