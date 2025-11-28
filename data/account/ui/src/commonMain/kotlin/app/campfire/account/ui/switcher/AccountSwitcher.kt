package app.campfire.account.ui.switcher

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.di.rememberComponent
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.asComposeIcon
import app.campfire.common.compose.icons.filled.Library
import app.campfire.common.compose.icons.icon
import app.campfire.common.compose.icons.rememberTentVectorPainter
import app.campfire.common.compose.icons.rounded.AccountSwitch
import app.campfire.common.compose.theme.PaytoneOneFontFamily
import app.campfire.core.coroutines.LoadState
import app.campfire.core.di.UserScope
import app.campfire.core.model.Library
import app.campfire.core.model.Tent
import campfire.data.account.ui.generated.resources.Res
import campfire.data.account.ui.generated.resources.libraries_error_message
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
  onClick: (eventSink: (AccountSwitcherUiEvent) -> Unit) -> Unit,
  modifier: Modifier = Modifier,
  component: AccountSwitcherComponent = rememberComponent(),
) {
  val presenter = remember(component) { component.presenterFactory() }
  val state = presenter.present()
  AccountSwitcher(
    state = state,
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
      useDynamicColors = state.useDynamicColors,
      serverName = { Text(serverName) },
      userName = { userName?.let { Text(it) } },
      onClick = onClick,
    ) {
      if (state.libraryState != null) {
        LibraryPicker(
          state = state.libraryState,
          onLibraryClick = { library ->
            state.eventSink(AccountSwitcherUiEvent.SelectLibrary(library))
          },
        )
      }
    }
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
  useDynamicColors: Boolean,
  serverName: @Composable () -> Unit,
  userName: @Composable () -> Unit,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit,
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onClick),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(
          start = 24.dp,
          top = 24.dp,
          bottom = 24.dp,
          // This accounts for the built-in IconButton padding
          end = 16.dp,
        ),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (useDynamicColors) {
        Image(
          rememberTentVectorPainter(),
          contentDescription = null,
          modifier = Modifier
            .size(TentIconSize),
        )
      } else {
        Image(
          tent.icon,
          contentDescription = null,
          modifier = Modifier
            .size(TentIconSize),
        )
      }

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

    content()
  }
}

@Composable
private fun LibraryPicker(
  state: LibraryState,
  onLibraryClick: (Library) -> Unit,
  modifier: Modifier = Modifier,
  containerColor: Color = MaterialTheme.colorScheme.primary,
  contentColor: Color = MaterialTheme.colorScheme.contentColorFor(containerColor),
) {
  var expanded by remember { mutableStateOf(false) }
  val shape = RoundedCornerShape(16.dp)
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
        trailingContent = {
          val iconRotation by animateFloatAsState(if (expanded) 180f else 0f)
          Icon(
            Icons.Rounded.ArrowDropDown,
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
          MaterialTheme.colorScheme.inversePrimary
        },
      ) {
        LibraryRow(
          library = library,
          selected = selected,
          onClick = { onLibraryClick(library) },
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
  trailingContent: @Composable (() -> Unit)? = null,
) {
  Row(
    modifier = modifier
      .clickable(enabled = onClick != null) { onClick?.invoke() }
      .fillMaxWidth()
      .padding(
        horizontal = 24.dp,
        vertical = 16.dp,
      ),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(library.icon.asComposeIcon(), contentDescription = null)
    Spacer(Modifier.width(16.dp))
    Text(
      text = library.name,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
      modifier = Modifier.weight(1f),
    )

    trailingContent?.let { content ->
      Spacer(Modifier.width(8.dp))
      content()
    }
  }
}

private val TentIconSize = 64.dp
