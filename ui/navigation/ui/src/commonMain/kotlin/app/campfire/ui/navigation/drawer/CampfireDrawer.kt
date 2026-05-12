package app.campfire.ui.navigation.drawer

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.DrawerState
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.di.rememberComponent
import app.campfire.common.compose.layout.NavigationType
import app.campfire.common.compose.layout.navigationType
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.core.di.UserScope
import app.campfire.core.reflect.instanceOf
import app.campfire.ui.navigation.HomeNavigationItem
import app.campfire.ui.navigation.NavigationPresenterFactory
import app.campfire.ui.theming.api.screen.ThemePickerScreen
import app.campfire.updates.AppUpdateWidget
import app.campfire.whatsnew.api.WhatsNewWidgetProvider
import app.campfire.whatsnew.api.screen.ChangelogScreen
import campfire.ui.navigation.ui.generated.resources.Res
import campfire.ui.navigation.ui.generated.resources.action_change_theme
import com.r0adkll.kimchi.annotations.ContributesTo
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.Screen
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@ContributesTo(UserScope::class)
interface CampfireDrawerComponent {
  val appUpdateWidget: AppUpdateWidget
  val whatsNewWidget: WhatsNewWidgetProvider
  val navigationPresenterFactory: NavigationPresenterFactory
}

@Composable
fun CampfireDrawer(
  rootScreen: Screen,
  drawerState: DrawerState,
  navigator: Navigator,
  accountSwitcher: @Composable () -> Unit,
  modifier: Modifier = Modifier,
  component: CampfireDrawerComponent = rememberComponent(),
) {
  val scope = rememberCoroutineScope()
  val presenter = remember(component) {
    component.navigationPresenterFactory()
  }
  val drawerItems = presenter.presentDrawer()

  DrawerSheet(
    modifier = modifier,
  ) {
    accountSwitcher()

    component.whatsNewWidget.Content(
      onClick = {
        navigator.goTo(ChangelogScreen)
        scope.launch {
          drawerState.close()
        }
      },
      modifier = Modifier.padding(
        horizontal = 16.dp,
      ),
    )

    Spacer(Modifier.height(8.dp))

    drawerItems.forEach { item ->
      DestinationListItem(
        item = item,
        rootScreen = rootScreen,
        onClick = {
          navigator.goTo(item.screen)
          scope.launch {
            drawerState.close()
          }
        },
      )
    }

    Spacer(Modifier.weight(1f))

    val changeThemeLabel = stringResource(Res.string.action_change_theme)
    IconButtonTooltip(
      text = changeThemeLabel,
      modifier = Modifier
        .align(Alignment.End)
        .padding(
          horizontal = 16.dp,
          vertical = 8.dp,
        ),
    ) {
      FilledTonalIconButton(
        shapes = IconButtonDefaults.shapes(),
        colors = IconButtonDefaults.filledTonalIconButtonColors(),
        onClick = {
          navigator.goTo(ThemePickerScreen)
          scope.launch {
            drawerState.close()
          }
        },
      ) {
        Icon(Icons.Rounded.Palette, contentDescription = changeThemeLabel)
      }
    }

    component.appUpdateWidget.Content(
      Modifier
        .fillMaxWidth(),
    )
  }
}

@Composable
private fun DestinationListItem(
  item: HomeNavigationItem,
  rootScreen: Screen,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  NavigationDrawerItem(
    icon = {
      Icon(
        imageVector = item.iconImageVector,
        contentDescription = item.contentDescription,
      )
    },
    label = { Text(text = item.label) },
    selected = item.screen.instanceOf(rootScreen::class),
    onClick = onClick,
    modifier = modifier
      .padding(
        horizontal = 16.dp,
      ),
  )
}

@Composable
private fun DrawerSheet(
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit,
) {
  val navigationType = LocalWindowSizeClass.current.navigationType
  if (navigationType == NavigationType.Drawer) {
    PermanentDrawerSheet(
      content = content,
      modifier = modifier,
    )
  } else {
    ModalDrawerSheet(
      content = content,
      modifier = modifier,
    )
  }
}
