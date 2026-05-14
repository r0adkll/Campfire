package app.campfire.podcasts.ui.find.composables

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import app.campfire.common.compose.widgets.IconButtonTooltip
import campfire.features.podcasts.ui.generated.resources.Res
import campfire.features.podcasts.ui.generated.resources.find_episodes_back
import campfire.features.podcasts.ui.generated.resources.find_episodes_clear
import campfire.features.podcasts.ui.generated.resources.find_episodes_placeholder
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SearchInput(
  textFieldState: TextFieldState,
  onBackClick: () -> Unit,
  modifier: Modifier = Modifier,
  focusRequester: FocusRequester = remember { FocusRequester() },
) {
  val placeholder = stringResource(Res.string.find_episodes_placeholder)
  val clearLabel = stringResource(Res.string.find_episodes_clear)
  SearchBar(
    windowInsets = WindowInsets(),
    modifier = modifier,
    inputField = {
      SearchBarDefaults.InputField(
        state = textFieldState,
        textStyle = MaterialTheme.typography.bodyLarge,
        onSearch = {},
        expanded = false,
        onExpandedChange = {},
        placeholder = { Text(placeholder) },
        leadingIcon = {
          val backLabel = stringResource(Res.string.find_episodes_back)
          IconButtonTooltip(text = backLabel) {
            IconButton(onClick = onBackClick) {
              Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = backLabel,
              )
            }
          }
        },
        trailingIcon = {
          if (textFieldState.text.isNotEmpty()) {
            IconButtonTooltip(text = clearLabel) {
              IconButton(onClick = { textFieldState.clearText() }) {
                Icon(
                  Icons.Rounded.Close,
                  contentDescription = clearLabel,
                )
              }
            }
          }
        },
        modifier = Modifier
          .focusRequester(focusRequester),
      )
    },
    expanded = false,
    onExpandedChange = {},
  ) {}
}
