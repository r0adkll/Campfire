package app.campfire.ui.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.DeveloperMode
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NotificationsPaused
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.CampfireWindowInsets
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.layout.LocalSupportingContentState
import app.campfire.common.compose.layout.SupportingContentState
import app.campfire.common.compose.layout.isSupportingPaneEnabled
import app.campfire.common.compose.widgets.CampfireTopAppBar
import app.campfire.common.screens.SettingsScreen
import app.campfire.core.di.UserScope
import app.campfire.core.isDebug
import app.campfire.ui.settings.composables.SettingPaneListItem
import app.campfire.ui.settings.panes.AboutPane
import app.campfire.ui.settings.panes.AccountPane
import app.campfire.ui.settings.panes.AppearancePane
import app.campfire.ui.settings.panes.DeveloperPane
import app.campfire.ui.settings.panes.DownloadsPane
import app.campfire.ui.settings.panes.LocalPaneState
import app.campfire.ui.settings.panes.PaneState
import app.campfire.ui.settings.panes.PlaybackPane
import app.campfire.ui.settings.panes.SleepPane
import campfire.features.settings.ui.generated.resources.Res
import campfire.features.settings.ui.generated.resources.setting_about_subtitle
import campfire.features.settings.ui.generated.resources.setting_about_title
import campfire.features.settings.ui.generated.resources.setting_account_subtitle
import campfire.features.settings.ui.generated.resources.setting_account_title
import campfire.features.settings.ui.generated.resources.setting_appearance_subtitle
import campfire.features.settings.ui.generated.resources.setting_appearance_title
import campfire.features.settings.ui.generated.resources.setting_developer_subtitle
import campfire.features.settings.ui.generated.resources.setting_developer_title
import campfire.features.settings.ui.generated.resources.setting_downloads_subtitle
import campfire.features.settings.ui.generated.resources.setting_downloads_title
import campfire.features.settings.ui.generated.resources.setting_playback_subtitle
import campfire.features.settings.ui.generated.resources.setting_playback_title
import campfire.features.settings.ui.generated.resources.setting_sleep_subtitle
import campfire.features.settings.ui.generated.resources.setting_sleep_title
import campfire.features.settings.ui.generated.resources.settings_title
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import org.jetbrains.compose.resources.stringResource

@CircuitInject(SettingsScreen::class, UserScope::class)
@Composable
fun SettingsUi(
  screen: SettingsScreen,
  state: SettingsUiState,
  modifier: Modifier = Modifier,
) {
  val windowSizeClass by rememberUpdatedState(LocalWindowSizeClass.current)
  val supportingContentState by rememberUpdatedState(LocalSupportingContentState.current)
  val isTwoPaneLayout = (
    windowSizeClass.isSupportingPaneEnabled &&
      supportingContentState == SupportingContentState.Closed
    ) ||
    windowSizeClass.widthSizeClass >= WindowWidthSizeClass.ExtraLarge

  var currentSettingsPane by rememberSaveable {
    mutableStateOf(
      when (screen.page) {
        SettingsScreen.Page.Root -> null
        SettingsScreen.Page.Account -> SettingsPane.Account
        SettingsScreen.Page.Appearance -> SettingsPane.Appearance
        SettingsScreen.Page.Downloads -> SettingsPane.Downloads
        SettingsScreen.Page.Playback -> SettingsPane.Playback
        SettingsScreen.Page.Sleep -> SettingsPane.Sleep
        SettingsScreen.Page.About -> SettingsPane.About
        SettingsScreen.Page.Developer -> SettingsPane.Developer
      },
    )
  }

  if (isTwoPaneLayout) {
    TwoPaneLayout(
      state = state,
      pane = currentSettingsPane,
      onPaneClick = { currentSettingsPane = it },
      modifier = modifier,
    )
  } else if (screen.page == SettingsScreen.Page.Root) {
    OnePaneLayout(
      state = state,
      pane = currentSettingsPane,
      onPaneClick = { state.eventSink(SettingsUiEvent.SettingsPaneClick(it)) },
      modifier = modifier,
    )
  } else {
    OnlyPaneLayout(
      state = state,
      pane = currentSettingsPane!!,
      onBackClick = { state.eventSink(SettingsUiEvent.Back) },
    )
  }
}

@Composable
private fun TwoPaneLayout(
  state: SettingsUiState,
  pane: SettingsPane?,
  onPaneClick: (SettingsPane?) -> Unit,
  modifier: Modifier = Modifier,
) {
  val forcedPane = pane ?: SettingsPane.Account
  Row(
    modifier = modifier.fillMaxSize(),
  ) {
    SettingsRootPane(
      hideTopBar = true,
      pane = forcedPane,
      onPaneClick = onPaneClick,
      onBackClick = { state.eventSink(SettingsUiEvent.Back) },
      modifier = Modifier
        .padding(top = 16.dp)
        .fillMaxHeight()
        .weight(1f),
    )

    Spacer(Modifier.width(16.dp))

    Surface(
      contentColor = MaterialTheme.colorScheme.surfaceContainerHigh,
      tonalElevation = 4.dp,
      shape = RoundedCornerShape(
        topStart = 16.dp,
        bottomStart = 16.dp,
      ),
      modifier = Modifier
        .padding(
          top = 16.dp,
          bottom = 16.dp,
        )
        .fillMaxHeight()
        .weight(1f),
    ) {
      CompositionLocalProvider(
        LocalPaneState provides PaneState.Double,
      ) {
        AnimatedContent(
          targetState = forcedPane,
          modifier = Modifier.fillMaxSize(),
        ) { currentSettingPane ->
          SettingPaneContent(
            state = state,
            settingsPane = currentSettingPane,
            onBackClick = {}, // Does nothing in two-pane layout
            modifier = Modifier
              .fillMaxSize(),
          )
        }
      }
    }
  }
}

@Composable
private fun OnePaneLayout(
  state: SettingsUiState,
  pane: SettingsPane?,
  onPaneClick: (SettingsPane) -> Unit,
  modifier: Modifier = Modifier,
) {
  SettingsRootPane(
    pane = pane,
    onPaneClick = onPaneClick,
    onBackClick = { state.eventSink(SettingsUiEvent.Back) },
    modifier = modifier
      .fillMaxSize(),
  )
}

@Composable
private fun OnlyPaneLayout(
  state: SettingsUiState,
  pane: SettingsPane,
  onBackClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier
      .background(MaterialTheme.colorScheme.surface)
      .fillMaxSize(),
  ) {
    SettingPaneContent(
      state = state,
      settingsPane = pane,
      onBackClick = onBackClick,
    )
  }
}

@Composable
private fun SettingsRootPane(
  pane: SettingsPane?,
  onPaneClick: (SettingsPane) -> Unit,
  onBackClick: () -> Unit,
  modifier: Modifier = Modifier,
  hideTopBar: Boolean = false,
) {
  Scaffold(
    topBar = {
      if (!hideTopBar) {
        CampfireTopAppBar(
          title = { Text(stringResource(Res.string.settings_title)) },
          navigationIcon = {
            val windowSizeClass = LocalWindowSizeClass.current
            if (!windowSizeClass.isSupportingPaneEnabled) {
              IconButton(
                onClick = onBackClick,
              ) {
                Icon(
                  Icons.AutoMirrored.Rounded.ArrowBack,
                  contentDescription = null,
                )
              }
            }
          },
        )
      }
    },
    modifier = modifier,
    contentWindowInsets = CampfireWindowInsets,
  ) { paddingValues ->
    Column(
      verticalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier
        .padding(paddingValues)
        .verticalScroll(rememberScrollState()),
    ) {
      // Account
      SettingPaneListItem(
        selected = pane == SettingsPane.Account && hideTopBar,
        icon = {
          Icon(
            Icons.Rounded.AccountCircle,
            contentDescription = null,
          )
        },
        title = { Text(stringResource(Res.string.setting_account_title)) },
        subtitle = { Text(stringResource(Res.string.setting_account_subtitle)) },
        onClick = {
          onPaneClick(SettingsPane.Account)
        },
      )

      // Appearance
      SettingPaneListItem(
        selected = pane == SettingsPane.Appearance && hideTopBar,
        icon = {
          Icon(
            Icons.Rounded.Palette,
            contentDescription = null,
          )
        },
        title = { Text(stringResource(Res.string.setting_appearance_title)) },
        subtitle = { Text(stringResource(Res.string.setting_appearance_subtitle)) },
        onClick = {
          onPaneClick(SettingsPane.Appearance)
        },
      )

      // Appearance
      SettingPaneListItem(
        selected = pane == SettingsPane.Downloads && hideTopBar,
        icon = {
          Icon(
            Icons.Rounded.Download,
            contentDescription = null,
          )
        },
        title = { Text(stringResource(Res.string.setting_downloads_title)) },
        subtitle = { Text(stringResource(Res.string.setting_downloads_subtitle)) },
        onClick = {
          onPaneClick(SettingsPane.Downloads)
        },
      )

      // Playback
      SettingPaneListItem(
        selected = pane == SettingsPane.Playback && hideTopBar,
        icon = {
          Icon(
            Icons.AutoMirrored.Rounded.VolumeUp,
            contentDescription = null,
          )
        },
        title = { Text(stringResource(Res.string.setting_playback_title)) },
        subtitle = { Text(stringResource(Res.string.setting_playback_subtitle)) },
        onClick = {
          onPaneClick(SettingsPane.Playback)
        },
      )

      // Sleep
      SettingPaneListItem(
        selected = pane == SettingsPane.Sleep && hideTopBar,
        icon = {
          Icon(
            Icons.Rounded.NotificationsPaused,
            contentDescription = null,
          )
        },
        title = { Text(stringResource(Res.string.setting_sleep_title)) },
        subtitle = { Text(stringResource(Res.string.setting_sleep_subtitle)) },
        onClick = {
          onPaneClick(SettingsPane.Sleep)
        },
      )

      // About
      SettingPaneListItem(
        selected = pane == SettingsPane.About && hideTopBar,
        icon = {
          Icon(
            Icons.Rounded.Info,
            contentDescription = null,
          )
        },
        title = { Text(stringResource(Res.string.setting_about_title)) },
        subtitle = { Text(stringResource(Res.string.setting_about_subtitle)) },
        onClick = {
          onPaneClick(SettingsPane.About)
        },
      )

      // Developer - DEBUG ONLY
      if (isDebug) {
        SettingPaneListItem(
          selected = pane == SettingsPane.Developer && hideTopBar,
          icon = {
            Icon(
              Icons.Rounded.DeveloperMode,
              contentDescription = null,
            )
          },
          title = { Text(stringResource(Res.string.setting_developer_title)) },
          subtitle = { Text(stringResource(Res.string.setting_developer_subtitle)) },
          onClick = {
            onPaneClick(SettingsPane.Developer)
          },
        )
      }
    }
  }
}

@Composable
private fun SettingPaneContent(
  state: SettingsUiState,
  settingsPane: SettingsPane,
  onBackClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  when (settingsPane) {
    SettingsPane.Account -> AccountPane(
      state = state,
      onBackClick = onBackClick,
      modifier = modifier,
    )

    SettingsPane.Appearance -> AppearancePane(
      state = state,
      onBackClick = onBackClick,
      modifier = modifier,
    )

    SettingsPane.Downloads -> DownloadsPane(
      state = state,
      onBackClick = onBackClick,
      modifier = modifier,
    )

    SettingsPane.Playback -> PlaybackPane(
      state = state,
      onBackClick = onBackClick,
      modifier = modifier,
    )

    SettingsPane.Sleep -> SleepPane(
      state = state,
      onBackClick = onBackClick,
      modifier = modifier,
    )

    SettingsPane.About -> AboutPane(
      state = state,
      onBackClick = onBackClick,
      modifier = modifier,
    )

    SettingsPane.Developer -> DeveloperPane(
      state = state,
      onBackClick = onBackClick,
      modifier = modifier,
    )
  }
}
