// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.management.ui.editor

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf

/**
 * A list of names edited as chips, such as a book's genres, starting from [initial], with [input]
 * holding what's typed for the next one. A name isn't added twice, ignoring case, since a name
 * that only differs in its capitals is almost always the same one.
 */
@Stable
class TokenListState(private val initial: List<String>) {

  val tokens = mutableStateListOf<String>().apply { addAll(initial) }
  val input = TextFieldState()

  /** The names, with what's typed but not yet added at the end, which a save keeps too */
  val values: List<String>
    get() {
      val pending = input.text.toString().trim()
      return if (pending.isEmpty() || tokens.containsIgnoringCase(pending)) tokens.toList() else tokens + pending
    }

  /**
   * Adds [name], trimmed, unless it's blank or already there. A name the list started with keeps
   * the capitals it had, so taking one out and typing it back changes nothing.
   */
  fun add(name: String): Boolean {
    val trimmed = name.trim()
    if (trimmed.isEmpty() || tokens.containsIgnoringCase(trimmed)) return false
    tokens += initial.firstOrNull { it.equals(trimmed, ignoreCase = true) } ?: trimmed
    return true
  }

  /** Adds what's typed as a name, spelled as it's [knownAs] if that's given, and clears the typing */
  fun commitInput(knownAs: String? = null) {
    val typed = input.text.toString()
    if (typed.isBlank()) return
    add(knownAs ?: typed)
    input.clearText()
  }

  fun remove(name: String) {
    tokens.remove(name)
  }

  /** Adds each of [names] that isn't there yet */
  fun addAll(names: List<String>) {
    names.forEach { add(it) }
  }

  /** Swaps the names for [names], dropping what's typed */
  fun replaceWith(names: List<String>) {
    tokens.clear()
    input.clearText()
    addAll(names)
  }

  /** Takes the last name back into the typing, to change it, when nothing's being typed */
  fun editLast(): Boolean {
    if (input.text.isNotEmpty() || tokens.isEmpty()) return false
    input.setTextAndPlaceCursorAtEnd(tokens.removeAt(tokens.lastIndex))
    return true
  }
}

internal fun Iterable<String>.containsIgnoringCase(name: String): Boolean = any { it.equals(name, ignoreCase = true) }
