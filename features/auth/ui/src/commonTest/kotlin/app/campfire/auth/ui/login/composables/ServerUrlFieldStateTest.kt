// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.ui.login.composables

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.test.Test
import kotlin.test.assertEquals

class ServerUrlFieldStateTest {

  @Test
  fun `insert appends at cursor`() {
    val state = ServerUrlFieldState("192.168.1.50")

    val result = state.insert(":13378")

    assertEquals("192.168.1.50:13378", result)
    assertEquals(TextRange("192.168.1.50:13378".length), state.value.selection)
  }

  @Test
  fun `insert at mid-text cursor position`() {
    val state = ServerUrlFieldState("https://.local")
    state.value = state.value.copy(selection = TextRange("https://".length))

    state.insert("192.168.")

    assertEquals("https://192.168..local", state.value.text)
    assertEquals(TextRange("https://192.168.".length), state.value.selection)
  }

  @Test
  fun `insert replaces selection`() {
    val state = ServerUrlFieldState("http://old-host:1337")
    state.value = state.value.copy(
      selection = TextRange("http://".length, "http://old-host".length),
    )

    state.insert("192.168.")

    assertEquals("http://192.168.:1337", state.value.text)
    assertEquals(TextRange("http://192.168.".length), state.value.selection)
  }

  @Test
  fun `scheme insert on empty text`() {
    val state = ServerUrlFieldState("")

    val result = state.insert("https://")

    assertEquals("https://", result)
    assertEquals(TextRange("https://".length), state.value.selection)
  }

  @Test
  fun `scheme insert prepends to bare host`() {
    val state = ServerUrlFieldState("abs.example.com")

    val result = state.insert("https://")

    assertEquals("https://abs.example.com", result)
  }

  @Test
  fun `scheme insert replaces existing scheme`() {
    val state = ServerUrlFieldState("https://192.168.1.50")

    val result = state.insert("http://")

    assertEquals("http://192.168.1.50", result)
  }

  @Test
  fun `scheme replace keeps cursor within bounds`() {
    val state = ServerUrlFieldState("https://x")
    state.value = state.value.copy(selection = TextRange(2))

    state.insert("http://")

    assertEquals("http://x", state.value.text)
    val cursor = state.value.selection.start
    assertEquals(true, cursor >= "http://".length && cursor <= "http://x".length)
  }

  @Test
  fun `external text change resets via TextFieldValue assignment`() {
    val state = ServerUrlFieldState("https://abs.example.com")

    state.value = TextFieldValue("", TextRange(0))
    val result = state.insert("https://")

    assertEquals("https://", result)
  }
}
