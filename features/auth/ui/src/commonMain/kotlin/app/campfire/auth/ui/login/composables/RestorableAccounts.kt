// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.ui.login.composables

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.campfire.account.api.RestorableAccount
import app.campfire.auth.ui.composables.TitleDivider
import app.campfire.auth.ui.shared.AuthSharedTransitionKey
import app.campfire.auth.ui.shared.AuthSharedTransitionKey.ElementType.RestoreAccount
import app.campfire.auth.ui.shared.AuthSharedTransitionKey.ElementType.RestoreTitle
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Close
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.ui.theming.api.AppTheme
import app.campfire.ui.theming.api.AppThemeImage
import campfire.features.auth.ui.generated.resources.Res
import campfire.features.auth.ui.generated.resources.action_dismiss_restorable_account
import campfire.features.auth.ui.generated.resources.login_restorable_accounts_title
import com.slack.circuit.sharedelements.PreviewSharedElementTransitionLayout
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import com.slack.circuit.sharedelements.SharedElementTransitionScope.AnimatedScope.Navigation
import org.jetbrains.compose.resources.stringResource

/**
 * The divider titling [RestorableAccounts], shared between the welcome and login screens so it
 * moves with them.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun RestorableAccountsTitle(
  modifier: Modifier = Modifier,
) = SharedElementTransitionScope {
  TitleDivider(
    title = stringResource(Res.string.login_restorable_accounts_title),
    modifier = modifier.sharedElement(
      sharedContentState = rememberSharedContentState(AuthSharedTransitionKey(RestoreTitle)),
      animatedVisibilityScope = requireAnimatedScope(Navigation),
    ),
  )
}

/**
 * Accounts signed in before the app was reinstalled. Picking one prefills the login form. The
 * app theme is restored with them, so its icon marks each account as the user's own.
 *
 * Each card is a shared element, so the ones on screen move between the welcome and login screens.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun RestorableAccounts(
  accounts: List<RestorableAccount>,
  theme: AppTheme,
  onSelect: (RestorableAccount) -> Unit,
  onDismiss: (RestorableAccount) -> Unit,
  modifier: Modifier = Modifier,
  contentPadding: PaddingValues = PaddingValues(),
  showTitle: Boolean = false,
) = SharedElementTransitionScope {
  Column(modifier) {
    if (showTitle) {
      Text(
        text = stringResource(Res.string.login_restorable_accounts_title),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier
          .padding(contentPadding)
          .padding(bottom = 8.dp),
      )
    }
    LazyRow(
      contentPadding = contentPadding,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      items(accounts, key = { it.userId + it.serverUrl }) { account ->
        RestorableAccountCard(
          account = account,
          theme = theme,
          onClick = { onSelect(account) },
          onDismiss = { onDismiss(account) },
          modifier = Modifier.sharedElement(
            sharedContentState = rememberSharedContentState(
              AuthSharedTransitionKey(RestoreAccount, id = account.userId + account.serverUrl),
            ),
            animatedVisibilityScope = requireAnimatedScope(Navigation),
          ),
        )
      }
    }
  }
}

@Composable
private fun RestorableAccountCard(
  account: RestorableAccount,
  theme: AppTheme,
  onClick: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    onClick = onClick,
    // Outlined pills, to set them apart from the filled "Add a campsite" card
    shape = MaterialTheme.shapes.large,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = modifier.widthIn(max = 280.dp),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(
        start = 12.dp,
      ),
    ) {
      AppThemeImage(
        appTheme = theme,
        modifier = Modifier.size(24.dp),
      )
      Column(
        modifier = Modifier
          .padding(
            start = 10.dp,
          )
          .weight(1f, fill = false),
      ) {
        Text(
          text = account.userName,
          style = MaterialTheme.typography.titleSmall,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = account.displayAddress,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      val dismissLabel = stringResource(Res.string.action_dismiss_restorable_account)
      IconButtonTooltip(text = dismissLabel) {
        IconButton(onClick = onDismiss) {
          Icon(
            CampfireIcons.Rounded.Close,
            contentDescription = dismissLabel,
            modifier = Modifier.size(18.dp),
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview
@Composable
private fun RestorableAccountsPreview() = PreviewSharedElementTransitionLayout {
  CampfireTheme {
    Surface {
      RestorableAccounts(
        theme = AppTheme.Fixed.Tent,
        accounts = listOf(
          RestorableAccount(
            serverUrl = "https://abs-longer-url.example.com",
            serverName = "Home",
            userId = "user-alice",
            userName = "alice",
          ),
          RestorableAccount(
            serverUrl = "https://books.example.org",
            serverName = "Neighborhood library",
            userId = "user-bob",
            userName = "bob",
          ),
        ),
        onSelect = {},
        onDismiss = {},
      )
    }
  }
}

// The address is what tells accounts apart, and the scheme is noise at card size
private val RestorableAccount.displayAddress: String
  get() = serverUrl.substringAfter("://").trimEnd('/')
