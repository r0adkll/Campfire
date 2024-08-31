package app.campfire.account.ui.switcher

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.icon
import app.campfire.common.compose.icons.rounded.AccountSwitch
import app.campfire.common.compose.theme.PaytoneOneFontFamily
import app.campfire.core.model.Server
import app.campfire.core.model.Tent
import app.campfire.core.model.User
import campfire.data.account.ui.generated.resources.Res
import campfire.data.account.ui.generated.resources.server_name_error
import campfire.data.account.ui.generated.resources.server_name_loading
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import org.jetbrains.compose.resources.stringResource

/**
 * Injectable typealias for the [AccountSwitcher] composable so that we can inject its
 * own presenter for managing the current account state to display
 */
typealias AccountSwitcher = @Composable (
  onClick: () -> Unit,
  modifier: Modifier,
) -> Unit

@Inject
@Composable
fun AccountSwitcher(
  presenter: AccountSwitcherPresenter,
  @Assisted onClick: () -> Unit,
  @Assisted modifier: Modifier = Modifier,
) {
  val state = presenter.present()
  AccountSwitcher(
    state = state,
    onClick = onClick,
    modifier = modifier,
  )
}

sealed interface AccountSwitcherState {
  data object Loading : AccountSwitcherState
  data class Loaded(
    val server: Server,
    val user: User,
  ) : AccountSwitcherState
  data object Error : AccountSwitcherState
}

@Composable
private fun AccountSwitcher(
  state: AccountSwitcherState,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val tent = when (state) {
    is AccountSwitcherState.Loaded -> state.server.tent
    else -> Tent.Default
  }

  val serverName = when (state) {
    is AccountSwitcherState.Loaded -> state.server.name
    AccountSwitcherState.Loading -> stringResource(Res.string.server_name_loading)
    AccountSwitcherState.Error -> stringResource(Res.string.server_name_error)
  }

  val userName = when (state) {
    is AccountSwitcherState.Loaded -> state.user.name
    else -> null
  }

  AccountSwitcher(
    tent = tent,
    serverName = { Text(serverName) },
    userName = { userName?.let { Text(it) } },
    onClick = onClick,
    modifier = modifier,
  )
}

@Composable
private fun AccountSwitcher(
  tent: Tent,
  serverName: @Composable () -> Unit,
  userName: @Composable () -> Unit,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(
        start = 24.dp,
        top = 24.dp,
        bottom = 24.dp,
        // This accounts for the built-in IconButton padding
        end = 16.dp,
      ),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Image(
      tent.icon,
      contentDescription = null,
      modifier = Modifier
        .size(TentIconSize),
    )

    Spacer(Modifier.width(16.dp))

    Column(
      modifier = Modifier
        .weight(1f),
    ) {
      ProvideTextStyle(
        MaterialTheme.typography.headlineSmall.copy(
          fontFamily = PaytoneOneFontFamily,
        ),
      ) {
        serverName()
      }
      ProvideTextStyle(MaterialTheme.typography.titleMedium) {
        userName()
      }
    }

    Spacer(Modifier.width(16.dp))

    IconButton(
      onClick = onClick,
    ) {
      Icon(
        Icons.Rounded.AccountSwitch,
        contentDescription = null,
      )
    }
  }
}

private val TentIconSize = 64.dp
