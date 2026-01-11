package app.campfire.auth.ui.login.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.ui.focus.FocusRequester.Companion.FocusRequesterFactory.component1
import androidx.compose.ui.focus.FocusRequester.Companion.FocusRequesterFactory.component2
import androidx.compose.ui.focus.FocusRequester.Companion.FocusRequesterFactory.component3
import androidx.compose.ui.focus.FocusRequester.Companion.FocusRequesterFactory.component4
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.campfire.auth.ui.login.AuthError
import app.campfire.auth.ui.login.ConnectionState
import app.campfire.auth.ui.shared.AuthSharedTransitionKey
import app.campfire.auth.ui.shared.AuthSharedTransitionKey.ElementType
import app.campfire.auth.ui.shared.AuthSharedTransitionKey.ElementType.Card
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.icon
import app.campfire.common.compose.icons.rounded.AssignmentGlobe
import app.campfire.common.compose.icons.rounded.Connected
import app.campfire.common.compose.icons.rounded.Disconnected
import app.campfire.common.compose.icons.rounded.Settings
import app.campfire.common.compose.theme.PaytoneOneFontFamily
import app.campfire.core.model.NetworkSettings
import app.campfire.core.model.Tent
import campfire.features.auth.ui.generated.resources.Res
import campfire.features.auth.ui.generated.resources.invalid_server_url
import campfire.features.auth.ui.generated.resources.label_login_error_auth
import campfire.features.auth.ui.generated.resources.label_login_error_network
import campfire.features.auth.ui.generated.resources.label_login_error_oauth
import campfire.features.auth.ui.generated.resources.label_password
import campfire.features.auth.ui.generated.resources.label_server_name_placeholder
import campfire.features.auth.ui.generated.resources.label_server_url
import campfire.features.auth.ui.generated.resources.label_username
import campfire.features.auth.ui.generated.resources.loading_server_url
import campfire.features.auth.ui.generated.resources.valid_server_url
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import com.slack.circuit.sharedelements.SharedElementTransitionScope.AnimatedScope.Navigation
import org.jetbrains.compose.resources.stringResource

@OptIn(
  ExperimentalComposeUiApi::class,
  ExperimentalSharedTransitionApi::class,
  ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
internal fun ServerCard(
  tent: Tent,
  onTentChange: (Tent) -> Unit,
  serverName: String,
  onServerNameChange: (String) -> Unit,
  serverUrl: String,
  onServerUrlChange: (String) -> Unit,
  networkSettings: NetworkSettings?,
  onEditNetworkSettingsClick: () -> Unit,
  username: String,
  onUsernameChange: (String) -> Unit,
  password: String,
  onPasswordChange: (String) -> Unit,
  onGo: () -> Unit,
  connectionState: ConnectionState?,
  authError: AuthError?,
  isAuthenticating: Boolean,
  modifier: Modifier = Modifier,
) = SharedElementTransitionScope {
  val (serverNameFocus, serverUrlFocus, usernameFocus, passwordFocus) = remember { FocusRequester.createRefs() }

  LaunchedEffect(isAuthenticating) {
    if (isAuthenticating) {
      serverNameFocus.freeFocus()
      serverUrlFocus.freeFocus()
      usernameFocus.freeFocus()
      passwordFocus.freeFocus()
    }
  }

  ElevatedCard(
    colors = CardDefaults.elevatedCardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ),
    modifier = modifier
      .sharedBounds(
        sharedContentState = rememberSharedContentState(AuthSharedTransitionKey(Card)),
        animatedVisibilityScope = requireAnimatedScope(Navigation),
      ),
  ) {
    Spacer(Modifier.size(16.dp))

    ServerNameAndIcon(
      enabled = !isAuthenticating,
      tent = tent,
      onTentChange = onTentChange,
      name = serverName,
      onNameChange = onServerNameChange,
      focusRequester = serverNameFocus,
      modifier = Modifier.padding(
        start = 16.dp,
        end = 16.dp,
      ),
    )

    Spacer(Modifier.size(16.dp))

    OutlinedTextField(
      enabled = !isAuthenticating,
      value = serverUrl,
      onValueChange = onServerUrlChange,
      label = { Text(stringResource(Res.string.label_server_url)) },
      leadingIcon = {
        Icon(
          when (connectionState) {
            is ConnectionState.Success -> CampfireIcons.Rounded.Connected
            else -> CampfireIcons.Rounded.Disconnected
          },
          contentDescription = null,
        )
      },
      supportingText = if (
        serverUrl.isNotBlank() &&
        connectionState != null &&
        connectionState !is ConnectionState.Success
      ) {
        {
          Text(
            when (connectionState) {
              ConnectionState.Loading -> stringResource(Res.string.loading_server_url)
              is ConnectionState.Error -> stringResource(Res.string.invalid_server_url)
              is ConnectionState.Success -> stringResource(Res.string.valid_server_url)
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
        autoCorrectEnabled = true,
        imeAction = ImeAction.Next,
      ),
      isError = connectionState is ConnectionState.Error,
      singleLine = true,
      modifier = Modifier
        .fillMaxWidth()
        .padding(
          horizontal = 16.dp,
        )
        .focusRequester(serverUrlFocus)
        .focusProperties {
          previous = serverNameFocus
          next = usernameFocus
        },
    )

    AnimatedVisibility(
      visible = serverUrl.isNotBlank(),
      modifier = Modifier
        .padding(horizontal = 16.dp)
        .align(Alignment.End),
    ) {
      val buttonSize = ButtonDefaults.ExtraSmallContainerHeight
      Row(
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (!networkSettings?.extraHeaders.isNullOrEmpty()) {
          NetworkSettingCounter(
            count = networkSettings.extraHeaders.size,
            buttonSize = buttonSize,
          )
        }

        Spacer(Modifier.size(4.dp))

        Button(
          enabled = !isAuthenticating,
          onClick = onEditNetworkSettingsClick,
          shapes = ButtonDefaults.shapes(
            shape = ButtonDefaults.squareShape,
          ),
          contentPadding = ButtonDefaults.contentPaddingFor(buttonSize),
          modifier = Modifier
            .heightIn(buttonSize),
        ) {
          Icon(
            CampfireIcons.Rounded.Settings,
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.iconSizeFor(buttonSize)),
          )
          Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(buttonSize)))
          Text(
            text = "Edit network settings",
            style = ButtonDefaults.textStyleFor(buttonSize),
          )
        }
      }
    }

    AnimatedVisibility(
      visible = connectionState is ConnectionState.Success,
    ) {
      val authMethodState = (connectionState as? ConnectionState.Success)?.authMethodState
      Column(
        Modifier.padding(
          start = 16.dp,
          end = 16.dp,
        ),
      ) {
        if (authMethodState?.passwordAuthEnabled == true) {
          OutlinedTextField(
            enabled = !isAuthenticating,
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
              .focusRequester(usernameFocus)
              .focusProperties {
                previous = serverUrlFocus
                next = passwordFocus
              }
              .semantics {
                contentType = ContentType.Username
              },
          )

          Spacer(Modifier.height(8.dp))

          var showPassword by remember { mutableStateOf(false) }
          OutlinedTextField(
            enabled = !isAuthenticating,
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
              .focusRequester(passwordFocus)
              .semantics {
                contentType = ContentType.Password
              },
          )
        }

        if (authError != null) {
          Spacer(Modifier.height(16.dp))

          Text(
            text = stringResource(
              when (authError) {
                AuthError.InvalidCredentials -> Res.string.label_login_error_auth
                AuthError.NetworkError -> Res.string.label_login_error_network
                AuthError.OAuthError -> Res.string.label_login_error_oauth
              },
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.error,
          )
        }
      }
    }

    Spacer(Modifier.size(16.dp))
  }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ServerNameAndIcon(
  enabled: Boolean,
  tent: Tent,
  onTentChange: (Tent) -> Unit,
  name: String,
  onNameChange: (String) -> Unit,
  focusRequester: FocusRequester,
  modifier: Modifier = Modifier,
) = SharedElementTransitionScope {
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
        modifier = Modifier
          .sharedElement(
            sharedContentState = rememberSharedContentState(AuthSharedTransitionKey(ElementType.Tent)),
            animatedVisibilityScope = requireAnimatedScope(Navigation),
          ),
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
        enabled = enabled,
        value = name,
        onValueChange = onNameChange,
        textStyle = MaterialTheme.typography.titleMedium.copy(
          fontFamily = PaytoneOneFontFamily,
          color = MaterialTheme.colorScheme.onSurface,
        ),
        singleLine = true,
        modifier = Modifier
          .focusRequester(focusRequester),
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NetworkSettingCounter(
  count: Int,
  buttonSize: Dp,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .heightIn(buttonSize)
      .border(
        width = 1.dp,
        color = MaterialTheme.colorScheme.outline,
        shape = ButtonDefaults.squareShape,
      )
      .padding(
        ButtonDefaults.contentPaddingFor(buttonSize),
      ),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      CampfireIcons.Rounded.AssignmentGlobe,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.outline,
      modifier = Modifier.size(ButtonDefaults.iconSizeFor(buttonSize)),
    )
    Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(buttonSize)))
    Text(
      text = "$count",
      style = ButtonDefaults.textStyleFor(buttonSize),
    )
  }
}
