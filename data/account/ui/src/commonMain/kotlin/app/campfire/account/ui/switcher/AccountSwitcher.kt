// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.account.ui.switcher

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.di.rememberComponent
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.asComposeIcon
import app.campfire.common.compose.icons.rounded.AccountSwitch
import app.campfire.common.compose.icons.rounded.ArrowDropDown
import app.campfire.common.compose.icons.rounded.Refresh
import app.campfire.common.compose.layout.isLandscapePhone
import app.campfire.common.compose.theme.PaytoneOneFontFamily
import app.campfire.common.compose.widgets.ConnectionIndicator
import app.campfire.common.compose.widgets.ConnectionState
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.core.coroutines.LoadState
import app.campfire.core.di.UserScope
import app.campfire.core.model.Library
import app.campfire.socket.SocketState
import app.campfire.ui.theming.api.AppTheme
import app.campfire.ui.theming.api.AppThemeImage
import campfire.data.account.ui.generated.resources.Res
import campfire.data.account.ui.generated.resources.action_switch_account
import campfire.data.account.ui.generated.resources.libraries_error_message
import campfire.data.account.ui.generated.resources.server_name_error
import campfire.data.account.ui.generated.resources.server_name_loading
import com.r0adkll.kimchi.annotations.ContributesTo
import org.jetbrains.compose.resources.stringResource

@ContributesTo(UserScope::class)
interface AccountSwitcherComponent {
  val accountSwitcherPresenterFactory: AccountSwitcherPresenterFactory
}

@Composable
fun AccountSwitcher(
  onClick: (eventSink: (AccountSwitcherUiEvent) -> Unit) -> Unit,
  modifier: Modifier = Modifier,
  shape: Shape = MaterialTheme.shapes.large,
  component: AccountSwitcherComponent = rememberComponent(),
) {
  val presenter = remember(component) { component.accountSwitcherPresenterFactory() }
  val state = presenter.present()
  AccountSwitcher(
    state = state,
    shape = shape,
    onClick = {
      onClick(state.eventSink)
    },
    modifier = modifier,
  )
}

@Composable
private fun AccountSwitcher(
  state: AccountSwitcherUiState,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  shape: Shape = MaterialTheme.shapes.large,
) {
  val windowSizeClass = LocalWindowSizeClass.current

  val serverName = when (val currentAccount = state.currentAccount) {
    is LoadState.Loaded -> currentAccount.data.name
    LoadState.Loading -> stringResource(Res.string.server_name_loading)
    LoadState.Error -> stringResource(Res.string.server_name_error)
  }

  val userName = when (val currentAccount = state.currentAccount) {
    is LoadState.Loaded -> currentAccount.data.user.name
    else -> null
  }

  val switcherConfig = if (windowSizeClass.isLandscapePhone) LandscapeSwitcherConfig else DefaultSwitcherConfig

  AccountCard(
    modifier = modifier
      .padding(16.dp),
    shape = shape,
  ) {
    AccountSwitcher(
      appTheme = state.theme,
      switcherConfig = switcherConfig,
      socketState = state.socketState,
      serverName = { Text(serverName) },
      userName = { userName?.let { Text(it) } },
      onClick = onClick,
      onRetryConnection = {
        state.eventSink(AccountSwitcherUiEvent.RetryConnection)
      },
    ) {
      if (state.libraryState != null) {
        LibraryPicker(
          state = state.libraryState,
          onLibraryClick = { library ->
            state.eventSink(AccountSwitcherUiEvent.SelectLibrary(library))
          },
          shape = shape,
        )
      }
    }
  }
}

@Composable
internal fun AccountCard(
  modifier: Modifier = Modifier,
  shape: Shape = MaterialTheme.shapes.large,
  content: @Composable ColumnScope.() -> Unit,
) {
  ElevatedCard(
    modifier = modifier,
    shape = shape,
    colors = CardDefaults.elevatedCardColors(
      containerColor = MaterialTheme.colorScheme.primaryContainer,
    ),
    content = content,
  )
}

@Composable
private fun AccountSwitcher(
  appTheme: AppTheme,
  socketState: SocketState,
  serverName: @Composable () -> Unit,
  userName: @Composable () -> Unit,
  onClick: () -> Unit,
  onRetryConnection: () -> Unit,
  switcherConfig: SwitcherConfig,
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit,
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onClick),
  ) {
    val bottomPadding by animateDpAsState(
      targetValue = if (socketState is SocketState.Failed) 8.dp else switcherConfig.padding,
    )
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(
          start = switcherConfig.padding,
          top = switcherConfig.padding,
          bottom = bottomPadding,
          // This accounts for the built-in IconButton padding
          end = switcherConfig.paddingEnd,
        ),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      AccountIcon(
        appTheme = appTheme,
        socketState = socketState,
        size = switcherConfig.iconSize,
        indicatorSize = switcherConfig.indicatorSize,
        indicatorOffset = switcherConfig.indicatorOffset,
        indicatorBorderColor = MaterialTheme.colorScheme.primaryContainer,
      )

      Spacer(Modifier.width(16.dp))

      Column(
        modifier = Modifier
          .weight(1f),
      ) {
        ProvideTextStyle(
          switcherConfig.serverNameTextStyle.copy(
            fontFamily = PaytoneOneFontFamily,
          ),
        ) {
          serverName()
        }
        ProvideTextStyle(switcherConfig.userNameTextStyle) {
          userName()
        }
      }

      Spacer(Modifier.width(16.dp))

      val switchAccountLabel = stringResource(Res.string.action_switch_account)
      IconButtonTooltip(text = switchAccountLabel) {
        IconButton(
          onClick = onClick,
        ) {
          Icon(
            CampfireIcons.Rounded.AccountSwitch,
            contentDescription = switchAccountLabel,
          )
        }
      }
    }

    AnimatedVisibility(
      visible = socketState is SocketState.Failed,
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(56.dp)
          .padding(
            start = 16.dp,
            end = 12.dp,
          ),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Disconnected: ${(socketState as? SocketState.Failed)?.reason}",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.error,
          fontStyle = FontStyle.Italic,
          modifier = Modifier.weight(1f),
        )

        Spacer(Modifier.width(8.dp))

        IconButton(
          onClick = onRetryConnection,
        ) {
          Icon(CampfireIcons.Rounded.Refresh, contentDescription = null)
        }
      }
    }

    content()
  }
}

/**
 * The theme icon for the current account with the socket connection dot in its corner.
 */
@Composable
internal fun AccountIcon(
  appTheme: AppTheme,
  socketState: SocketState,
  size: Dp,
  indicatorSize: Dp,
  indicatorOffset: Dp,
  indicatorBorderColor: Color,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier.size(size),
  ) {
    AppThemeImage(
      appTheme = appTheme,
      modifier = Modifier.size(size),
    )

    if (socketState !is SocketState.Disabled) {
      ConnectionIndicator(
        state = when (socketState) {
          is SocketState.Authenticated -> ConnectionState.Connected
          SocketState.Authenticating -> ConnectionState.Connecting
          SocketState.Connecting -> ConnectionState.Connecting
          SocketState.Disconnected -> ConnectionState.Disconnected
          is SocketState.Failed -> ConnectionState.Disconnected
          SocketState.Disabled -> error("guarded above")
        },
        size = indicatorSize,
        borderWidth = 3.dp,
        borderColor = indicatorBorderColor,
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(
            top = indicatorOffset,
            end = indicatorOffset,
          ),
      )
    }
  }
}

@Composable
internal fun LibraryPicker(
  state: LibraryState,
  onLibraryClick: (Library) -> Unit,
  modifier: Modifier = Modifier,
  containerColor: Color = MaterialTheme.colorScheme.primary,
  contentColor: Color = MaterialTheme.colorScheme.contentColorFor(containerColor),
  shape: Shape = MaterialTheme.shapes.large,
  rowStyle: LibraryRowStyle = DefaultLibraryRowStyle,
) {
  var expanded by remember { mutableStateOf(false) }
  Column(
    modifier = modifier
      .clip(shape)
      .background(
        color = containerColor,
        shape = shape,
      ),
  ) {
    CompositionLocalProvider(
      LocalContentColor provides contentColor,
    ) {
      LibraryRow(
        library = state.currentLibrary,
        onClick = { expanded = !expanded },
        rowStyle = rowStyle,
        trailingContent = {
          val iconRotation by animateFloatAsState(if (expanded) 180f else 0f)
          Icon(
            CampfireIcons.Rounded.ArrowDropDown,
            contentDescription = null,
            modifier = Modifier.rotate(iconRotation),
          )
        },
      )

      AnimatedVisibility(
        visible = expanded,
      ) {
        when (val allLibraries = state.allLibraries) {
          LoadState.Loading -> LibrariesLoading()
          LoadState.Error -> LibrariesError()
          is LoadState.Loaded -> LibrariesLoaded(
            currentLibrary = state.currentLibrary,
            libraries = allLibraries.data,
            onLibraryClick = { library ->
              onLibraryClick(library)
              expanded = false
            },
            rowStyle = rowStyle,
          )
        }
      }
    }
  }
}

@Composable
private fun LibrariesLoading(
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier.height(128.dp),
    contentAlignment = Alignment.Center,
  ) {
    CircularProgressIndicator()
  }
}

@Composable
private fun LibrariesError(
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .height(128.dp)
      .padding(horizontal = 24.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = stringResource(Res.string.libraries_error_message),
      style = MaterialTheme.typography.labelMedium,
    )
  }
}

@Composable
private fun LibrariesLoaded(
  currentLibrary: Library,
  libraries: List<Library>,
  onLibraryClick: (Library) -> Unit,
  modifier: Modifier = Modifier,
  rowStyle: LibraryRowStyle = DefaultLibraryRowStyle,
) {
  Column(
    modifier = modifier,
  ) {
    HorizontalDivider()

    libraries.forEach { library ->
      val selected = currentLibrary.id == library.id

      CompositionLocalProvider(
        LocalContentColor provides if (selected) {
          LocalContentColor.current
        } else {
          LocalContentColor.current.copy(alpha = 0.68f)
        },
      ) {
        LibraryRow(
          library = library,
          selected = selected,
          onClick = { onLibraryClick(library) },
          rowStyle = rowStyle,
          trailingContent = {
            RadioButton(
              selected = selected,
              onClick = null,
              colors = RadioButtonDefaults.colors(
                selectedColor = LocalContentColor.current,
                unselectedColor = LocalContentColor.current,
              ),
            )
          },
        )
      }
    }
  }
}

@Composable
private fun LibraryRow(
  library: Library,
  modifier: Modifier = Modifier,
  selected: Boolean = false,
  onClick: (() -> Unit)? = null,
  rowStyle: LibraryRowStyle = DefaultLibraryRowStyle,
  trailingContent: @Composable (() -> Unit)? = null,
) {
  Row(
    modifier = modifier
      .clickable(enabled = onClick != null) { onClick?.invoke() }
      .fillMaxWidth()
      .padding(
        horizontal = rowStyle.horizontalPadding,
        vertical = rowStyle.verticalPadding,
      ),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(library.icon.asComposeIcon(), contentDescription = null)
    Spacer(Modifier.width(rowStyle.iconSpacing))
    Text(
      text = library.name,
      style = rowStyle.textStyle,
      fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.weight(1f),
    )

    trailingContent?.let { content ->
      Spacer(Modifier.width(8.dp))
      content()
    }
  }
}

/** Sizing for the rows of a [LibraryPicker]. */
data class LibraryRowStyle(
  val horizontalPadding: Dp,
  val verticalPadding: Dp,
  val iconSpacing: Dp,
  val textStyle: TextStyle,
)

val DefaultLibraryRowStyle: LibraryRowStyle
  @Composable get() = LibraryRowStyle(
    horizontalPadding = 24.dp,
    verticalPadding = 16.dp,
    iconSpacing = 16.dp,
    textStyle = MaterialTheme.typography.titleMedium,
  )

/** Tighter rows for the account switcher in the wide navigation rail. */
val CompactLibraryRowStyle: LibraryRowStyle
  @Composable get() = LibraryRowStyle(
    horizontalPadding = 16.dp,
    verticalPadding = 12.dp,
    iconSpacing = 12.dp,
    textStyle = MaterialTheme.typography.titleSmall,
  )

data class SwitcherConfig(
  val iconSize: Dp,
  val padding: Dp,
  val paddingEnd: Dp,
  val indicatorSize: Dp,
  val indicatorOffset: Dp,
  val serverNameTextStyle: TextStyle,
  val userNameTextStyle: TextStyle,
)

val DefaultSwitcherConfig: SwitcherConfig
  @Composable get() = SwitcherConfig(
    iconSize = 64.dp,
    padding = 24.dp,
    paddingEnd = 16.dp,
    indicatorSize = 12.dp,
    indicatorOffset = 6.dp,
    serverNameTextStyle = MaterialTheme.typography.headlineSmall,
    userNameTextStyle = MaterialTheme.typography.titleMedium,
  )

val LandscapeSwitcherConfig: SwitcherConfig
  @Composable get() = SwitcherConfig(
    iconSize = 40.dp,
    padding = 16.dp,
    paddingEnd = 8.dp,
    indicatorSize = 8.dp,
    indicatorOffset = 3.dp,
    serverNameTextStyle = MaterialTheme.typography.titleLarge,
    userNameTextStyle = MaterialTheme.typography.titleSmall,
  )
