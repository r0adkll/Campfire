// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.logging

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class LogRedactionTest {

  @BeforeTest
  fun setup() {
    LogRedaction.enabled = true
    LogRedaction.clear()
  }

  @AfterTest
  fun teardown() {
    LogRedaction.enabled = true
    LogRedaction.clear()
  }

  @Test
  fun generic_pass_scrubs_any_url_authority_but_keeps_the_path() {
    val result = LogRedaction.redact(
      "REQUEST: https://abs.myserver.net:13378/api/items/li_abc123/cover failed",
    )

    assertThat(result).isEqualTo("REQUEST: https://<redacted>/api/items/li_abc123/cover failed")
  }

  @Test
  fun generic_pass_scrubs_websocket_urls() {
    val result = LogRedaction.redact("connecting to wss://abs.myserver.net/socket.io")

    assertThat(result).isEqualTo("connecting to wss://<redacted>/socket.io")
  }

  @Test
  fun generic_pass_scrubs_multiple_urls_in_one_message() {
    val result = LogRedaction.redact(
      "redirect from http://one.example.com/a to https://two.example.com/b",
    )

    assertThat(result).doesNotContain("one.example.com")
    assertThat(result).doesNotContain("two.example.com")
  }

  @Test
  fun registered_server_url_is_replaced_with_a_stable_token() {
    LogRedaction.registerServerUrl("https://abs.myserver.net:13378/")

    val first = LogRedaction.redact("session key: user1::https://abs.myserver.net:13378")
    val second = LogRedaction.redact("still https://abs.myserver.net:13378 here")

    assertThat(first).doesNotContain("abs.myserver.net")
    assertThat(second).doesNotContain("abs.myserver.net")

    val token = Regex("<server#([0-9a-f]+)>").find(first)?.groupValues?.get(1)
    assertThat(second).contains("<server#$token>")
  }

  @Test
  fun registered_host_is_scrubbed_from_bare_host_messages() {
    LogRedaction.registerServerUrl("https://abs.myserver.net:13378")

    val result = LogRedaction.redact("java.net.UnknownHostException: abs.myserver.net")

    assertThat(result).doesNotContain("abs.myserver.net")
    assertThat(result).contains("<host#")
  }

  @Test
  fun disabled_redaction_returns_the_message_untouched() {
    LogRedaction.registerServerUrl("https://abs.myserver.net")
    LogRedaction.enabled = false

    val message = "REQUEST: https://abs.myserver.net/api/me"
    assertThat(LogRedaction.redact(message)).isEqualTo(message)
  }

  @Test
  fun message_without_urls_is_unchanged() {
    val message = "Initializing: SessionSyncer"
    assertThat(LogRedaction.redact(message)).isEqualTo(message)
  }

  @Test
  fun loggable_url_keeps_only_the_path() {
    assertThat("https://abs.myserver.net:13378/api/items/li_abc123/cover?token=secret".loggableUrl)
      .isEqualTo("<server>/api/items/li_abc123/cover")
  }

  @Test
  fun loggable_url_of_a_bare_server_url_has_no_path() {
    assertThat("https://abs.myserver.net:13378".loggableUrl).isEqualTo("<server>")
  }
}
