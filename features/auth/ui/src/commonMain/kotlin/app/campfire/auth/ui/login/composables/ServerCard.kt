package app.campfire.auth.ui.login.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lan
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import app.campfire.auth.ui.login.AuthError
import app.campfire.auth.ui.login.ConnectionState
import app.campfire.common.compose.icons.icon
import app.campfire.common.compose.theme.PaytoneOneFontFamily
import app.campfire.core.model.Tent
import campfire.features.auth.ui.generated.resources.Res
import campfire.features.auth.ui.generated.resources.invalid_server_url
import campfire.features.auth.ui.generated.resources.label_login_error_auth
import campfire.features.auth.ui.generated.resources.label_login_error_network
import campfire.features.auth.ui.generated.resources.label_password
import campfire.features.auth.ui.generated.resources.label_server_name_placeholder
import campfire.features.auth.ui.generated.resources.label_server_url
import campfire.features.auth.ui.generated.resources.label_username
import campfire.features.auth.ui.generated.resources.loading_server_url
import campfire.features.auth.ui.generated.resources.valid_server_url
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun ServerCard(
  tent: Tent,
  onTentChange: (Tent) -> Unit,
  serverName: String,
  onServerNameChange: (String) -> Unit,
  serverUrl: String,
  onServerUrlChange: (String) -> Unit,
  username: String,
  onUsernameChange: (String) -> Unit,
  password: String,
  onPasswordChange: (String) -> Unit,
  onGo: () -> Unit,
  connectionState: ConnectionState?,
  authError: AuthError?,
  isAuthenticating: Boolean,
  modifier: Modifier = Modifier,
) {
  val focusRequester = remember { FocusRequester() }

  LaunchedEffect(isAuthenticating) {
    if (isAuthenticating) {
      focusRequester.freeFocus()
    }
  }

  ElevatedCard(
    colors = CardDefaults.elevatedCardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ),
    modifier = modifier,
  ) {
    ServerNameAndIcon(
      tent = tent,
      onTentChange = onTentChange,
      name = serverName,
      onNameChange = onServerNameChange,
      modifier = Modifier.padding(
        start = 16.dp,
        end = 16.dp,
        top = 16.dp,
      ),
    )

    OutlinedTextField(
      value = serverUrl,
      onValueChange = onServerUrlChange,
      label = { Text(stringResource(Res.string.label_server_url)) },
      leadingIcon = {
        Icon(
          Icons.Rounded.Lan,
          contentDescription = null,
        )
      },
      supportingText = if (serverUrl.isNotBlank() && connectionState != null) {
        {
          Text(
            when (connectionState) {
              ConnectionState.Loading -> stringResource(Res.string.loading_server_url)
              ConnectionState.Error -> stringResource(Res.string.invalid_server_url)
              ConnectionState.Success -> stringResource(Res.string.valid_server_url)
            },
          )
        }
      } else {
        null
      },
      trailingIcon = if (serverUrl.isNotBlank()) {
        {
          IconButton(
            onClick = { onServerUrlChange("") },
          ) {
            Icon(Icons.Rounded.Cancel, contentDescription = null)
          }
        }
      } else {
        null
      },
      keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Uri,
        imeAction = ImeAction.Next,
      ),
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
        .focusRequester(focusRequester),
    )

    AnimatedVisibility(
      visible = connectionState == ConnectionState.Success,
    ) {
      Column(
        Modifier.padding(
          start = 16.dp,
          end = 16.dp,
          bottom = 16.dp,
        ),
      ) {
        OutlinedTextField(
          value = username,
          onValueChange = onUsernameChange,
          label = { Text(stringResource(Res.string.label_username)) },
          leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
          keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
          ),
          modifier = Modifier
            .fillMaxWidth()
            .semantics {
              contentType = ContentType.Username
            },
        )

        Spacer(Modifier.height(8.dp))

        var showPassword by remember { mutableStateOf(false) }
        OutlinedTextField(
          value = password,
          onValueChange = onPasswordChange,
          label = { Text(stringResource(Res.string.label_password)) },
          leadingIcon = { Icon(Icons.Rounded.Password, contentDescription = null) },
          trailingIcon = {
            IconButton(
              onClick = { showPassword = !showPassword },
            ) {
              Icon(
                if (showPassword) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                contentDescription = null,
              )
            }
          },
          visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Go,
          ),
          keyboardActions = KeyboardActions(
            onGo = { onGo() },
          ),
          modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .semantics {
              contentType = ContentType.Password
            },
        )

        if (authError != null) {
          Spacer(Modifier.height(16.dp))

          Text(
            text = stringResource(
              when (authError) {
                AuthError.InvalidCredentials -> Res.string.label_login_error_auth
                AuthError.NetworkError -> Res.string.label_login_error_network
              },
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.error,
          )
        }
      }
    }
  }
}

@Composable
private fun ServerNameAndIcon(
  tent: Tent,
  onTentChange: (Tent) -> Unit,
  name: String,
  onNameChange: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    var showTentPickerMenu by remember { mutableStateOf(false) }
    Box(
      modifier = Modifier
        .clip(RoundedCornerShape(8.dp))
        .clickable {
          showTentPickerMenu = true
        },
    ) {
      Image(
        tent.icon,
        contentDescription = null,
      )
      Box(
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .size(24.dp)
          .background(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = CircleShape,
          )
          .clip(CircleShape),
      ) {
        Icon(
          Icons.Rounded.KeyboardArrowDown,
          contentDescription = null,
        )
      }

      DropdownMenu(
        expanded = showTentPickerMenu,
        onDismissRequest = { showTentPickerMenu = false },
        modifier = Modifier.padding(
          horizontal = 8.dp,
          vertical = 8.dp,
        ),
      ) {
        Tent.entries.forEach { tentOption ->
          Image(
            tentOption.icon,
            contentDescription = null,
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .clickable {
                onTentChange(tentOption)
                showTentPickerMenu = false
              },
          )
        }
      }
    }
    Spacer(Modifier.width(16.dp))
    Box {
      BasicTextField(
        value = name,
        onValueChange = onNameChange,
        textStyle = MaterialTheme.typography.titleMedium.copy(
          fontFamily = PaytoneOneFontFamily,
          color = MaterialTheme.colorScheme.onSurface,
        ),
        singleLine = true,
      )
      if (name.isEmpty()) {
        Text(
          text = stringResource(Res.string.label_server_name_placeholder),
          style = MaterialTheme.typography.titleMedium.copy(
            fontFamily = PaytoneOneFontFamily,
            fontStyle = FontStyle.Italic,
          ),
          modifier = Modifier.alpha(0.5f),
        )
      }
    }
  }
}
