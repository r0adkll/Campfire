// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.ui.login

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ServerUrlProbeTest {

  @Test
  fun `input with scheme is probed as-is`() {
    assertEquals(
      listOf("https://abs.example.com"),
      serverUrlProbeCandidates("https://abs.example.com"),
    )
    assertEquals(
      listOf("http://192.168.1.50:13378"),
      serverUrlProbeCandidates("http://192.168.1.50:13378"),
    )
  }

  @Test
  fun `bare public host tries https first`() {
    assertEquals(
      listOf("https://abs.example.com", "http://abs.example.com"),
      serverUrlProbeCandidates("abs.example.com"),
    )
  }

  @Test
  fun `bare private ip tries http first`() {
    assertEquals(
      listOf("http://192.168.1.50:13378", "https://192.168.1.50:13378"),
      serverUrlProbeCandidates("192.168.1.50:13378"),
    )
  }

  @Test
  fun `mdns host tries http first`() {
    assertEquals(
      listOf("http://audiobookshelf.local", "https://audiobookshelf.local"),
      serverUrlProbeCandidates("audiobookshelf.local"),
    )
  }

  @Test
  fun `lan host detection`() {
    assertTrue(isLikelyLanHost("localhost"))
    assertTrue(isLikelyLanHost("localhost:13378"))
    assertTrue(isLikelyLanHost("abs.local"))
    assertTrue(isLikelyLanHost("ABS.LOCAL:13378"))
    assertTrue(isLikelyLanHost("192.168.0.10"))
    assertTrue(isLikelyLanHost("10.0.0.5:13378"))
    assertTrue(isLikelyLanHost("172.16.0.1"))
    assertTrue(isLikelyLanHost("172.31.255.255:8080"))
    assertTrue(isLikelyLanHost("192.168.1.50/audiobookshelf"))

    assertFalse(isLikelyLanHost("abs.example.com"))
    assertFalse(isLikelyLanHost("172.32.0.1"))
    assertFalse(isLikelyLanHost("172.15.0.1"))
    assertFalse(isLikelyLanHost("110.0.0.5"))
    assertFalse(isLikelyLanHost("mylocal.example.com"))
  }

  @Test
  fun `port and path do not confuse host detection`() {
    assertTrue(isLikelyLanHost("10.1.2.3:13378/some/path"))
    assertFalse(isLikelyLanHost("example.com:10.5"))
  }
}
