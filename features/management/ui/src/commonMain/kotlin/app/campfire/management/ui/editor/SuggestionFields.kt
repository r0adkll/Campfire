// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.management.ui.editor

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuBoxScope
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldLabelPosition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Add
import app.campfire.common.compose.icons.rounded.Close
import campfire.features.management.ui.generated.resources.Res
import campfire.features.management.ui.generated.resources.cd_remove
import campfire.features.management.ui.generated.resources.suggestion_add
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Whether suggestions show as a row of chips under the field rather than a menu over the form,
 * for a window too short to fit a menu beside the keyboard, such as a phone on its side.
 */
internal val LocalInlineSuggestions = compositionLocalOf { false }

/**
 * A field of names, such as a book's genres, each a chip that a tap takes out. Typing suggests
 * the library's own names from [suggestions], and Done or Enter adds what's typed, as does leaving
 * the field. Backspace with nothing typed takes the last name back to change it.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
internal fun TokenField(
  state: TokenListState,
  label: StringResource,
  changed: Boolean,
  suggestions: List<String>,
  enabled: Boolean,
  tag: String,
  modifier: Modifier = Modifier,
  capitalization: KeyboardCapitalization = KeyboardCapitalization.Words,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val focused by interactionSource.collectIsFocusedAsState()
  var dismissedAt by remember { mutableStateOf<String?>(null) }

  val query = state.input.text.toString().trim()
  val tokens = state.tokens.toList()
  val matches = remember(query, suggestions, tokens) { suggestionsFor(query, suggestions, excluding = tokens) }
  val offersAdding = query.isNotEmpty() &&
    !tokens.containsIgnoringCase(query) &&
    matches.none { it.name.equals(query, ignoreCase = true) }
  val suggesting = enabled && focused && dismissedAt != query && (matches.isNotEmpty() || offersAdding)
  val inline = LocalInlineSuggestions.current

  // What's typed, spelled the way the library has it when it has it
  fun commit(knownAs: String? = null) {
    state.commitInput(knownAs ?: suggestions.firstOrNull { it.equals(query, ignoreCase = true) })
    dismissedAt = null
  }

  // Leaving the field adds what's typed, which a save would keep anyway
  LaunchedEffect(focused) {
    if (!focused) commit()
  }

  val bringIntoView = remember { BringIntoViewRequester() }
  KeepInViewWhileFocused(bringIntoView, focused, suggesting, tokens.size)

  val colors = OutlinedTextFieldDefaults.colors()
  val decorator = OutlinedTextFieldDefaults.decorator(
    state = state.input,
    enabled = enabled,
    lineLimits = TextFieldLineLimits.SingleLine,
    outputTransformation = null,
    interactionSource = interactionSource,
    labelPosition = TextFieldLabelPosition.Attached(alwaysMinimize = tokens.isNotEmpty()),
    label = { FieldLabel(text = stringResource(label), changed = changed) },
    contentPadding = if (tokens.isEmpty()) {
      OutlinedTextFieldDefaults.contentPadding()
    } else {
      OutlinedTextFieldDefaults.contentPadding(start = 12.dp, top = 8.dp, bottom = 8.dp)
    },
  )

  Column(modifier = modifier.bringIntoViewRequester(bringIntoView)) {
    ExposedDropdownMenuBox(
      expanded = suggesting && !inline,
      onExpandedChange = { open -> dismissedAt = if (open) null else query },
    ) {
      BasicTextField(
        state = state.input,
        enabled = enabled,
        lineLimits = TextFieldLineLimits.SingleLine,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
          color = when {
            !enabled -> colors.disabledTextColor
            focused -> colors.focusedTextColor
            else -> colors.unfocusedTextColor
          },
        ),
        cursorBrush = SolidColor(colors.cursorColor),
        keyboardOptions = KeyboardOptions(capitalization = capitalization, imeAction = ImeAction.Done),
        onKeyboardAction = { performDefault -> if (query.isEmpty()) performDefault() else commit() },
        interactionSource = interactionSource,
        decorator = TextFieldDecorator { innerTextField ->
          decorator.Decoration {
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              itemVerticalAlignment = Alignment.CenterVertically,
            ) {
              tokens.forEach { name ->
                key(name) {
                  TokenChip(name = name, enabled = enabled, onRemove = { state.remove(name) })
                }
              }
              Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier
                  .weight(1f)
                  .widthIn(min = MinTypingWidth),
              ) {
                innerTextField()
              }
            }
          }
        },
        modifier = Modifier
          // Room for the label that sits on the outline, as an OutlinedTextField makes, so the
          // field takes up all it draws and scrolls into view whole
          .padding(top = labelHalfHeight())
          .fillMaxWidth()
          .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, enabled)
          .onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            when (event.key) {
              Key.Backspace -> state.editLast()
              Key.Enter, Key.NumPadEnter -> query.isNotEmpty().also { if (it) commit() }
              Key.Escape -> suggesting.also { if (it) dismissedAt = query }
              else -> false
            }
          }
          .testTag(tag),
      )

      SuggestionsMenu(
        expanded = suggesting && !inline,
        matches = matches,
        adding = query.takeIf { offersAdding },
        onPick = { commit(knownAs = it) },
        onAdd = { commit() },
        onDismiss = { dismissedAt = query },
      )
    }

    if (suggesting && inline) {
      InlineSuggestions(
        matches = matches,
        adding = query.takeIf { offersAdding },
        onPick = { commit(knownAs = it) },
        onAdd = { commit() },
        modifier = Modifier.padding(top = 4.dp),
      )
    }
  }
}

/**
 * A text field that suggests the library's own names from [suggestions] while it's typed in, such
 * as its publishers, leaving out [excluding]. Picking one fills the field with it.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun SuggestingTextField(
  state: TextFieldState,
  suggestions: List<String>,
  enabled: Boolean,
  modifier: Modifier = Modifier,
  excluding: Collection<String> = emptyList(),
  field: @Composable (Modifier) -> Unit,
) {
  var focused by remember { mutableStateOf(false) }
  var dismissedAt by remember { mutableStateOf<String?>(null) }

  val typed = state.text.toString()
  val matches = remember(typed, suggestions, excluding) {
    // Once the field says exactly a name there's nothing left to suggest for it
    suggestionsFor(typed, suggestions, excluding).filterNot { it.name == typed.trim() }
  }
  val suggesting = enabled && focused && dismissedAt != typed && matches.isNotEmpty()
  val inline = LocalInlineSuggestions.current

  fun pick(name: String) {
    state.setTextAndPlaceCursorAtEnd(name)
    dismissedAt = name
  }

  val bringIntoView = remember { BringIntoViewRequester() }
  KeepInViewWhileFocused(bringIntoView, focused, suggesting)

  Column(modifier = modifier.bringIntoViewRequester(bringIntoView)) {
    ExposedDropdownMenuBox(
      expanded = suggesting && !inline,
      onExpandedChange = { open -> dismissedAt = if (open) null else typed },
    ) {
      field(
        Modifier
          .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, enabled)
          .onFocusChanged { focused = it.isFocused }
          .onPreviewKeyEvent { event ->
            val dismissing = event.type == KeyEventType.KeyDown && event.key == Key.Escape && suggesting
            if (dismissing) dismissedAt = typed
            dismissing
          },
      )

      SuggestionsMenu(
        expanded = suggesting && !inline,
        matches = matches,
        adding = null,
        onPick = ::pick,
        onAdd = {},
        onDismiss = { dismissedAt = typed },
      )
    }

    if (suggesting && inline) {
      InlineSuggestions(
        matches = matches,
        adding = null,
        onPick = ::pick,
        onAdd = {},
        modifier = Modifier.padding(top = 4.dp),
      )
    }
  }
}

/**
 * Keeps all of a field in view while it has the focus, as the keyboard comes up and as what
 * shows with it changes, not only the line being typed on.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun KeepInViewWhileFocused(
  requester: BringIntoViewRequester,
  focused: Boolean,
  vararg keys: Any?,
) {
  val keyboardHeight = if (focused) WindowInsets.ime.getBottom(LocalDensity.current) else 0
  LaunchedEffect(focused, keyboardHeight, *keys) {
    if (!focused) return@LaunchedEffect
    // Once the keyboard settles, so what's in view is what's left above it
    delay(KeyboardSettleMillis)
    requester.bringIntoView()
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExposedDropdownMenuBoxScope.SuggestionsMenu(
  expanded: Boolean,
  matches: List<Suggestion>,
  adding: String?,
  onPick: (String) -> Unit,
  onAdd: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  ExposedDropdownMenu(
    expanded = expanded,
    onDismissRequest = onDismiss,
    modifier = modifier.testTag("suggestions"),
  ) {
    matches.forEach { suggestion ->
      DropdownMenuItem(
        text = {
          Text(
            text = suggestion.highlighted(),
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        },
        onClick = { onPick(suggestion.name) },
        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
      )
    }
    if (adding != null) {
      DropdownMenuItem(
        text = {
          Text(
            text = stringResource(Res.string.suggestion_add, adding),
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        },
        leadingIcon = { Icon(CampfireIcons.Rounded.Add, contentDescription = null) },
        onClick = onAdd,
        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
      )
    }
  }
}

/** The suggestions as a row of chips to scroll through, for where a menu wouldn't fit */
@Composable
private fun InlineSuggestions(
  matches: List<Suggestion>,
  adding: String?,
  onPick: (String) -> Unit,
  onAdd: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    modifier = modifier
      .fillMaxWidth()
      .horizontalScroll(rememberScrollState())
      .testTag("suggestions"),
  ) {
    matches.forEach { suggestion ->
      SuggestionChip(
        onClick = { onPick(suggestion.name) },
        label = { Text(suggestion.highlighted(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
        modifier = Modifier.keepsFieldFocused(),
      )
    }
    if (adding != null) {
      AssistChip(
        onClick = onAdd,
        modifier = Modifier.keepsFieldFocused(),
        label = { Text(stringResource(Res.string.suggestion_add, adding), maxLines = 1) },
        leadingIcon = {
          Icon(
            imageVector = CampfireIcons.Rounded.Add,
            contentDescription = null,
            modifier = Modifier.size(AssistChipDefaults.IconSize),
          )
        },
      )
    }
  }
}

@Composable
private fun TokenChip(
  name: String,
  enabled: Boolean,
  onRemove: () -> Unit,
  modifier: Modifier = Modifier,
) {
  InputChip(
    selected = false,
    onClick = onRemove,
    enabled = enabled,
    label = { Text(name) },
    trailingIcon = {
      Icon(
        imageVector = CampfireIcons.Rounded.Close,
        contentDescription = stringResource(Res.string.cd_remove),
        modifier = Modifier.size(InputChipDefaults.IconSize),
      )
    },
    modifier = modifier,
  )
}

/**
 * A suggestion doesn't take the focus when it's tapped, which would leave the field and add what's
 * typed before the suggestion could replace it.
 */
private fun Modifier.keepsFieldFocused(): Modifier = focusProperties { canFocus = false }

/** Half the height of a field's label once it sits on the outline */
@Composable
private fun labelHalfHeight(): Dp = with(LocalDensity.current) {
  MaterialTheme.typography.bodySmall.lineHeight.toDp() / 2
}

/** The name with what was typed in bold */
private fun Suggestion.highlighted(): AnnotatedString = buildAnnotatedString {
  append(name)
  addStyle(SpanStyle(fontWeight = FontWeight.Bold), match.first, match.last + 1)
}

private val MinTypingWidth = 64.dp
private const val KeyboardSettleMillis = 150L
