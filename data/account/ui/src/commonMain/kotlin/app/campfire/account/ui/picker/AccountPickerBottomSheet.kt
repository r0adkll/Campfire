package app.campfire.account.ui.picker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.di.rememberComponent
import app.campfire.common.compose.icons.filled.PersonAdd
import app.campfire.common.compose.icons.filled.ShieldPerson
import app.campfire.common.compose.icons.icon
import app.campfire.common.compose.widgets.CampsiteIcon
import app.campfire.core.coroutines.LoadState
import app.campfire.core.di.UserScope
import app.campfire.core.model.Server
import campfire.data.account.ui.generated.resources.Res
import campfire.data.account.ui.generated.resources.account_picker_logout_action_confirm
import campfire.data.account.ui.generated.resources.account_picker_logout_action_dismiss
import campfire.data.account.ui.generated.resources.account_picker_logout_text_format
import campfire.data.account.ui.generated.resources.account_picker_logout_title
import campfire.data.account.ui.generated.resources.account_picker_sheet_add_account
import campfire.data.account.ui.generated.resources.account_picker_sheet_error_message
import campfire.data.account.ui.generated.resources.account_picker_sheet_title
import com.r0adkll.kimchi.annotations.ContributesTo
import com.slack.circuit.overlay.OverlayHost
import com.slack.circuit.overlay.OverlayNavigator
import com.slack.circuitx.overlays.BottomSheetOverlay
import org.jetbrains.compose.resources.stringResource

@ContributesTo(UserScope::class)
interface AccountPickerComponent {
  val accountPickerPresenterFactory: AccountPickerPresenterFactory
}

sealed interface AccountPickerResult {
  data object None : AccountPickerResult
  data object AddAccount : AccountPickerResult
  data class SwitchAccount(val server: Server) : AccountPickerResult
}

suspend fun OverlayHost.showAccountPicker(): AccountPickerResult {
  return show(
    BottomSheetOverlay<Unit, AccountPickerResult>(
      model = Unit,
      onDismiss = { AccountPickerResult.None },
      sheetShape = RoundedCornerShape(
        topStart = 32.dp,
        topEnd = 32.dp,
      ),
      skipPartiallyExpandedState = true,
    ) { _, overlayNavigator ->
      AccountPickerContent(
        title = { Text(stringResource(Res.string.account_picker_sheet_title)) },
        overlayNavigator = overlayNavigator,
      )
    },
  )
}

@Composable
private fun AccountPickerContent(
  title: @Composable () -> Unit,
  overlayNavigator: OverlayNavigator<AccountPickerResult>,
  modifier: Modifier = Modifier,
  component: AccountPickerComponent = rememberComponent(),
) {
  Column(
    modifier = modifier,
  ) {
    Box(
      Modifier
        .padding(16.dp)
        .align(Alignment.CenterHorizontally),
    ) {
      ProvideTextStyle(
        MaterialTheme.typography.titleLarge.copy(
          fontWeight = FontWeight.SemiBold,
        ),
      ) {
        title()
      }
    }

    val presenter = remember(component) { component.accountPickerPresenterFactory() }
    val state = presenter.present()

    AccountPickerContent(
      accounts = state.accountState,
      onAccountClick = { server ->
        overlayNavigator.finish(AccountPickerResult.SwitchAccount(server))
      },
      onAddAccountClick = {
        overlayNavigator.finish(AccountPickerResult.AddAccount)
      },
      onLogout = { server ->
        state.eventSink(AccountPickerUiEvent.Logout(server))
      },
    )
  }
}

@Composable
private fun AccountPickerContent(
  accounts: LoadState<out AccountState>,
  onAccountClick: (Server) -> Unit,
  onAddAccountClick: () -> Unit,
  onLogout: (Server) -> Unit,
  modifier: Modifier = Modifier,
) {
  when (accounts) {
    is LoadState.Loading -> LoadingContent(modifier)
    is LoadState.Error -> ErrorContent(modifier)
    is LoadState.Loaded -> LoadedContent(
      accountState = accounts.data,
      onAccountClick = onAccountClick,
      onAddAccountClick = onAddAccountClick,
      onLogout = onLogout,
      modifier = modifier,
    )
  }
}

@Composable
private fun LoadingContent(
  modifier: Modifier = Modifier,
) {
  NonLoadedContent(modifier) {
    CircularProgressIndicator()
  }
}

@Composable
private fun ErrorContent(
  modifier: Modifier = Modifier,
) {
  NonLoadedContent(modifier) {
    Text(
      text = stringResource(Res.string.account_picker_sheet_error_message),
      textAlign = TextAlign.Center,
      modifier = Modifier.padding(horizontal = 32.dp),
    )
  }
}

@Composable
private fun NonLoadedContent(
  modifier: Modifier = Modifier,
  content: @Composable BoxScope.() -> Unit,
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .defaultMinSize(minHeight = 300.dp),
    contentAlignment = Alignment.Center,
    content = content,
  )
}

@Composable
private fun LoadedContent(
  accountState: AccountState,
  onAccountClick: (Server) -> Unit,
  onAddAccountClick: () -> Unit,
  onLogout: (Server) -> Unit,
  modifier: Modifier = Modifier,
) {
  var logoutConfirmation by remember { mutableStateOf<Server?>(null) }

  LazyColumn(
    modifier = modifier,
    contentPadding = PaddingValues(
      bottom = 32.dp,
    ),
  ) {
    items(
      items = accountState.all,
      key = { it.user.id },
    ) { server ->
      val isCurrent = server.user.id == accountState.current.user.id
      AccountListItem(
        server = server,
        isCurrent = isCurrent,
        onClick = {
          onAccountClick(server)
        },
        onLogout = {
          logoutConfirmation = server
        },
        modifier = Modifier.animateItem(),
      )
    }

    item {
      AddAccountListItem(
        onClick = onAddAccountClick,
        modifier = Modifier
          .padding(top = 16.dp)
          .padding(horizontal = 16.dp, vertical = 8.dp)
          .animateItem(),
      )
    }
  }

  if (logoutConfirmation != null) {
    LogoutConfirmationDialog(
      server = logoutConfirmation!!,
      onDismiss = { logoutConfirmation = null },
      onConfirm = {
        onLogout(logoutConfirmation!!)
        logoutConfirmation = null
      },
    )
  }
}

private val TentIconSize = 40.dp

@Composable
private fun AccountListItem(
  server: Server,
  isCurrent: Boolean,
  onClick: () -> Unit,
  onLogout: () -> Unit,
  modifier: Modifier = Modifier,
) {
  ListItem(
    modifier = modifier.clickable(
      enabled = !isCurrent,
      onClick = onClick,
    ),
    leadingContent = {
      CampsiteIcon(
        tent = server.tent,
        hasFire = isCurrent,
        modifier = Modifier.size(TentIconSize),
      )
    },
    headlineContent = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(server.user.name)
        if (isCurrent) {
          Spacer(Modifier.width(4.dp))
          Icon(
            Icons.Filled.ShieldPerson,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary,
          )
        }
      }
    },
    supportingContent = { Text(server.url) },
    trailingContent = {
      // TODO: We should add a confirmation state here similar to bookmark deletions
      IconButton(onClick = onLogout) {
        Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
      }
    },
    colors = ListItemDefaults.colors(
      containerColor = Color.Transparent,
    ),
  )
}

@Composable
private fun AddAccountListItem(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Button(
    modifier = modifier.fillMaxWidth(),
    onClick = onClick,
    contentPadding = ButtonDefaults.TextButtonWithIconContentPadding,
  ) {
    Icon(
      Icons.Filled.PersonAdd,
      contentDescription = null,
      modifier = Modifier.size(ButtonDefaults.IconSize),
    )
    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
    Text(stringResource(Res.string.account_picker_sheet_add_account))
  }
}

@Composable
private fun LogoutConfirmationDialog(
  server: Server,
  onDismiss: () -> Unit,
  onConfirm: () -> Unit,
  modifier: Modifier = Modifier,
) {
  AlertDialog(
    modifier = modifier,
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        Icons.AutoMirrored.Rounded.Logout,
        contentDescription = null,
      )
    },
    title = { Text(stringResource(Res.string.account_picker_logout_title)) },
    text = { Text(stringResource(Res.string.account_picker_logout_text_format, server.name)) },
    confirmButton = {
      TextButton(onClick = onConfirm) {
        Text(stringResource(Res.string.account_picker_logout_action_confirm))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(Res.string.account_picker_logout_action_dismiss))
      }
    },
  )
}
