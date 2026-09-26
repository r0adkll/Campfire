// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.settings

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test

class CustomHeadersTest {

  private val headers = mapOf("CF-Access-Client-Id" to "abc", "X-Token" to "secret")

  @Test
  fun `adding a header keeps the others`() {
    assertThat(headers.withHeader(null, "X-New", "1"))
      .isEqualTo(headers + ("X-New" to "1"))
  }

  @Test
  fun `editing a header can rename it`() {
    assertThat(headers.withHeader("X-Token", "X-Auth", "secret2"))
      .isEqualTo(mapOf("CF-Access-Client-Id" to "abc", "X-Auth" to "secret2"))
  }

  @Test
  fun `a differently cased name replaces rather than duplicates`() {
    assertThat(headers.withHeader(null, "x-token", "new"))
      .isEqualTo(mapOf("CF-Access-Client-Id" to "abc", "x-token" to "new"))
  }

  @Test
  fun `names and values are trimmed`() {
    assertThat(emptyMap<String, String>().withHeader(null, "  X-Token ", " v "))
      .isEqualTo(mapOf("X-Token" to "v"))
  }

  @Test
  fun `valid headers pass`() {
    assertThat(validateHeader("CF-Access-Client-Secret", "a.b-c_d")).isNull()
  }

  @Test
  fun `names must be tokens`() {
    assertThat(validateHeader(" ", "v")).isEqualTo(HeaderError.MissingName)
    assertThat(validateHeader("X Token", "v")).isEqualTo(HeaderError.InvalidName)
    assertThat(validateHeader("X:Token", "v")).isEqualTo(HeaderError.InvalidName)
  }

  @Test
  fun `values must be present, single line, and storable`() {
    assertThat(validateHeader("X-Token", "  ")).isEqualTo(HeaderError.MissingValue)
    assertThat(validateHeader("X-Token", "a\nb")).isEqualTo(HeaderError.InvalidValue)
    assertThat(validateHeader("X-Token", "a;|;b")).isEqualTo(HeaderError.InvalidValue)
    assertThat(validateHeader("X-Token", "a:|:b")).isEqualTo(HeaderError.InvalidValue)
  }
}
