package app.campfire.account.ui.switcher

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.di.rememberComponent
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.icon
import app.campfire.common.compose.icons.rounded.AccountSwitch
import app.campfire.common.compose.theme.PaytoneOneFontFamily
import app.campfire.core.coroutines.LoadState
import app.campfire.core.di.UserScope
import app.campfire.core.model.Tent
import campfire.data.account.ui.generated.resources.Res
import campfire.data.account.ui.generated.resources.server_name_error
import campfire.data.account.ui.generated.resources.server_name_loading
import com.r0adkll.kimchi.annotations.ContributesTo
import org.jetbrains.compose.resources.stringResource

@ContributesTo(UserScope::class)
interface AccountSwitcherComponent {
  val presenterFactory: AccountSwitcherPresenterFactory
}

@Composable
fun AccountSwitcher(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  component: AccountSwitcherComponent = rememberComponent(),
) {
  val presenter = remember(component) { component.presenterFactory() }
  val state = presenter.present()
  AccountSwitcher(
    state = state,
    onClick = onClick,
    modifier = modifier,
  )
}

@Composable
private fun AccountSwitcher(
  state: AccountSwitcherUiState,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val tent = when (val currentAccount = state.currentAccount) {
    is LoadState.Loaded -> currentAccount.data.tent
    else -> Tent.Default
  }

  val serverName = when (val currentAccount = state.currentAccount) {
    is LoadState.Loaded -> currentAccount.data.name
    LoadState.Loading -> stringResource(Res.string.server_name_loading)
    LoadState.Error -> stringResource(Res.string.server_name_error)
  }

  val userName = when (val currentAccount = state.currentAccount) {
    is LoadState.Loaded -> currentAccount.data.user.name
    else -> null
  }

  AccountCard(
    modifier = modifier
      .padding(16.dp),
  ) {
    AccountSwitcher(
      tent = tent,
      serverName = { Text(serverName) },
      userName = { userName?.let { Text(it) } },
      onClick = onClick,
    )
  }
}

@Composable
private fun AccountCard(
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit,
) {
  ElevatedCard(
    modifier = modifier,
    colors = CardDefaults.elevatedCardColors(
      containerColor = MaterialTheme.colorScheme.primaryContainer,
    ),
    content = content,
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
        CampfireIcons.Rounded.AccountSwitch,
        contentDescription = null,
      )
    }
  }
}

private val TentIconSize = 64.dp
