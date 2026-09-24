// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test

class ApiExceptionTest {

  @Test
  fun `an error status means the server answered`() {
    assertThat(ApiException(500).isServerUnreachable).isFalse()
    assertThat(ApiException(404).isServerUnreachable).isFalse()
  }

  @Test
  fun `a missing login is not a reachability failure`() {
    assertThat(AuthorizationException().isServerUnreachable).isFalse()
  }

  @Test
  fun `a transport failure means the server never answered`() {
    assertThat(RuntimeException("Connection refused").isServerUnreachable).isTrue()
  }
}
