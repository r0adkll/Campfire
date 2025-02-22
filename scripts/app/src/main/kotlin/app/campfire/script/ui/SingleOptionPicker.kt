package app.campfire.script.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.campfire.script.theme.primaryDark
import app.campfire.script.theme.secondaryDark
import app.campfire.script.ui.composables.BorderedTitledBox
import com.jakewharton.mosaic.LocalTerminal
import com.jakewharton.mosaic.layout.KeyEvent
import com.jakewharton.mosaic.layout.background
import com.jakewharton.mosaic.layout.height
import com.jakewharton.mosaic.layout.onKeyEvent
import com.jakewharton.mosaic.layout.padding
import com.jakewharton.mosaic.layout.width
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.text.SpanStyle
import com.jakewharton.mosaic.text.buildAnnotatedString
import com.jakewharton.mosaic.text.withStyle
import com.jakewharton.mosaic.ui.Alignment
import com.jakewharton.mosaic.ui.Box
import com.jakewharton.mosaic.ui.Color
import com.jakewharton.mosaic.ui.Column
import com.jakewharton.mosaic.ui.Text
import com.jakewharton.mosaic.ui.TextStyle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.awaitCancellation

private val DefaultBackgroundColor = Color(40, 42, 54)

/**
 * Show a Mosaic terminal UI application for selecting from a list of options including
 * fuzzy searching.
 * @param options The list of options to select from.
 * @param onOptionSelected The callback to invoke when an option is selected.
 * @param modifier The modifier to apply to the UI.
 * @param titleColor The color of the title text.
 * @param borderColor The color of the border.
 * @param backgroundColor The color of the background.
 */
@Composable
fun SingleOptionPicker(
  title: String,
  options: ImmutableList<String>,
  onOptionSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
  titleColor: Color = secondaryDark,
  borderColor: Color = secondaryDark,
  backgroundColor: Color = DefaultBackgroundColor,
) {
  val terminal = LocalTerminal.current
  var isExit by remember { mutableStateOf(false) }

  var query by remember { mutableStateOf("") }
  var selectedOption by remember { mutableStateOf<String?>(null) }

  val filteredOptions = remember(options, query) {
    options.filter { option ->
      query.isEmpty() || option.contains(query, ignoreCase = true)
    }
  }

  LaunchedEffect(filteredOptions) {
    if (
      filteredOptions.isNotEmpty() &&
      (selectedOption == null || filteredOptions.indexOf(selectedOption) == -1)
    ) {
      selectedOption = filteredOptions.firstOrNull()
    }
  }

  BorderedTitledBox(
    title = title,
    titleColor = titleColor,
    borderColor = borderColor,
    modifier = modifier
      .width(terminal.size.width)
      .height(terminal.size.height - 1)
      .background(backgroundColor)
      .onKeyEvent {
        if (isExitKeyEvent(it)) {
          isExit = true
          return@onKeyEvent true
        }

        when (it) {
          KeyEvent("ArrowUp") -> {
            val currentIndex = filteredOptions.indexOf(selectedOption)
            if (currentIndex > 0) {
              selectedOption = filteredOptions[currentIndex - 1]
            }
            true
          }

          KeyEvent("ArrowDown") -> {
            val currentIndex = filteredOptions.indexOf(selectedOption)
            if (currentIndex < filteredOptions.size - 1) {
              selectedOption = filteredOptions[currentIndex + 1]
            }
            true
          }

          KeyEvent("Tab"),
          KeyEvent(" "),
          KeyEvent("Enter"),
          -> {
            selectedOption?.let(onOptionSelected)
            isExit = true
            true
          }

          else -> false
        }
      },
  ) {
    Column {
      filteredOptions
        .take(terminal.size.height - 2)
        .forEach { option ->
          val isSelected = selectedOption == option
          Text(
            value = buildString {
              if (isSelected) {
                append("▶\uFE0E ")
              } else {
                append("  ")
              }
              append(option)
            },
            color = if (isSelected) primaryDark else Color.Unspecified,
            textStyle = if (isSelected) TextStyle.Italic else TextStyle.Unspecified,
          )
        }
    }
    SearchBar(
      value = query,
      onValueChange = { query = it },
      modifier = Modifier
        .height(1)
        .padding(horizontal = 1)
        .align(Alignment.BottomStart),
    )
  }

  // Control when we exit the term ui
  if (!isExit) {
    LaunchedEffect(Unit) {
      awaitCancellation()
    }
  }
}

@Composable
private fun SearchBar(
  value: String,
  onValueChange: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .onKeyEvent {
        if (isExitKeyEvent(it)) {
          // Propagate this event to the parent
          false
        } else if (it.key.length == 1) {
          onValueChange(value + it.key)
          true
        } else if (it == KeyEvent("Backspace") && value.isNotEmpty()) {
          onValueChange(value.substring(0, value.length - 1))
          true
        } else {
          false
        }
      },
  ) {
    Text(
      value = buildAnnotatedString {
        withStyle(SpanStyle(textStyle = TextStyle.Bold)) {
          append("Filter: ")
        }
        withStyle(SpanStyle(textStyle = TextStyle.Dim)) {
          append(value)
        }
      },
    )
  }
}

private fun isExitKeyEvent(event: KeyEvent): Boolean {
  return event == KeyEvent("Escape") ||
    event == KeyEvent("c", ctrl = true) ||
    event == KeyEvent("z", ctrl = true)
}
