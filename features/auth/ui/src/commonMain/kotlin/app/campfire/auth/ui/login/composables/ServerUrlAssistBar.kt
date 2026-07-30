// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.ui.login.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

/**
 * UI-side editing state for the server URL field. The presenter remains the source of
 * truth for the URL *text*; this holder tracks the cursor/selection and focus so the
 * keyboard assist bar can insert snippets at the caret.
 */
@Stable
internal class ServerUrlFieldState(initialText: String) {

  var value by mutableStateOf(TextFieldValue(initialText, TextRange(initialText.length)))

  var isFocused by mutableStateOf(false)

  /**
   * Insert [snippet] at the current cursor (replacing any selection), returning the new
   * text. Scheme snippets (`https://`) instead replace an existing scheme so tapping
   * them never produces `https://http://…`.
   */
  fun insert(snippet: String): String {
    val current = value
    value = if (snippet.endsWith("://")) {
      val existingScheme = SchemeRegex.find(current.text)?.value.orEmpty()
      val rest = current.text.removePrefix(existingScheme)
      val cursor = (current.selection.max - existingScheme.length + snippet.length)
        .coerceIn(snippet.length, snippet.length + rest.length)
      TextFieldValue(snippet + rest, TextRange(cursor))
    } else {
      val selection = current.selection
      val text = current.text.replaceRange(selection.min, selection.max, snippet)
      TextFieldValue(text, TextRange(selection.min + snippet.length))
    }
    return value.text
  }

  companion object {
    private val SchemeRegex = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://")
  }
}

@Composable
internal fun rememberServerUrlFieldState(initialText: String): ServerUrlFieldState {
  return remember { ServerUrlFieldState(initialText) }
}

/**
 * A keyboard accessory bar of quick-insert snippets for the server URL field, covering
 * the parts of a web or LAN address that are the most tedious to type on a soft
 * keyboard. Only visible while the URL field is focused and the IME is open, so it
 * never appears with hardware keyboards.
 */
@Composable
internal fun ServerUrlAssistBar(
  urlState: ServerUrlFieldState,
  onUrlChange: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
  AnimatedVisibility(
    visible = urlState.isFocused && imeVisible,
    enter = slideInVertically { it } + fadeIn(),
    exit = slideOutVertically { it } + fadeOut(),
    modifier = modifier,
  ) {
    Surface(
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        modifier = Modifier
          .horizontalScroll(rememberScrollState())
          .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        UrlSnippets.forEach { snippet ->
          SuggestionChip(
            onClick = { onUrlChange(urlState.insert(snippet)) },
            colors = SuggestionChipDefaults.suggestionChipColors(
              containerColor = MaterialTheme.colorScheme.surface,
            ),
            label = {
              Text(
                text = snippet,
                fontFamily = FontFamily.Monospace,
              )
            },
          )
        }
      }
    }
  }
}

// The Audiobookshelf default port is 13378
private val UrlSnippets = listOf("https://", "http://", "192.168.", ".local", ":13378")
