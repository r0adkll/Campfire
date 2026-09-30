// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.credentials

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class PasswordCredentialIdTest {

  @Test
  fun `the id is the username at the server's host`() {
    assertThat(passwordCredentialId("https://abs.example.com", "alice"))
      .isEqualTo("alice@abs.example.com")
  }

  @Test
  fun `scheme, path and case don't change the id`() {
    assertThat(passwordCredentialId("http://ABS.example.com/audiobookshelf/", "alice"))
      .isEqualTo("alice@abs.example.com")
  }

  @Test
  fun `the port is kept, since it can be a different server`() {
    assertThat(passwordCredentialId("http://192.168.1.50:13378", "bob"))
      .isEqualTo("bob@192.168.1.50:13378")
  }

  @Test
  fun `a username that is an email keeps its own at sign`() {
    assertThat(passwordCredentialId("https://abs.example.com", "alice@mail.example"))
      .isEqualTo("alice@mail.example@abs.example.com")
  }
}
