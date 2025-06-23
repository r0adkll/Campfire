package app.campfire.ui.appbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import app.campfire.common.compose.navigation.LocalSearchView
import app.campfire.common.compose.navigation.localDrawerOpener
import app.campfire.common.compose.theme.PaytoneOneFontFamily
import app.campfire.common.compose.util.withDensity
import app.campfire.common.compose.widgets.AppBarState
import app.campfire.common.compose.widgets.AppBarViewEvent
import app.campfire.common.compose.widgets.CampfireAppBar
import app.campfire.core.model.Library
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

// Injectable typealias
@OptIn(ExperimentalMaterial3Api::class)
typealias CampfireAppBar = @Composable (
  modifier: Modifier,
  scrollBehavior: TopAppBarScrollBehavior?,
) -> Unit

@OptIn(ExperimentalMaterial3Api::class)
@Inject
@Composable
fun CampfireAppBar(
  presenter: CampfireAppbarPresenter,
  @Assisted modifier: Modifier = Modifier,
  @Assisted scrollBehavior: TopAppBarScrollBehavior?,
) {
  val drawerOpener = localDrawerOpener()
  val searchViewNavigationState = LocalSearchView.current

  var showLibrariesPopup by remember { mutableStateOf(false) }

  val state = presenter.present()
  CampfireAppBar(
    state = state,
    onNavigationClick = drawerOpener,
    onSearchClick = { searchViewNavigationState?.navigateToSearchView() },
    onTitleClick = {
      showLibrariesPopup = true
    },
    modifier = modifier,
    scrollBehavior = scrollBehavior,
  )

  if (showLibrariesPopup) {
    LibrariesPopup(
      currentLibrary = (state.library as? AppBarState.LibraryState.Loaded)?.library,
      libraries = state.allLibraries,
      onLibraryClick = {
        state.eventSink(AppBarViewEvent.LibrarySelected(it))
        showLibrariesPopup = false
      },
      onDismissRequest = {
        showLibrariesPopup = false
      },
    )
  }
}

@Composable
private fun LibrariesPopup(
  currentLibrary: Library?,
  libraries: List<Library>,
  onLibraryClick: (Library) -> Unit,
  onDismissRequest: () -> Unit,
) {
  val scope = rememberCoroutineScope()
  var visible by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    visible = true
  }

  val dismissPopup: () -> Unit = {
    scope.launch {
      visible = false
      delay(200)
      onDismissRequest()
    }
  }

  Popup(
    alignment = Alignment.TopCenter,
    offset = IntOffset(0, withDensity { (-16).dp.roundToPx() }),
    onDismissRequest = dismissPopup,
  ) {
    Box(
      modifier = Modifier.fillMaxWidth(),
      contentAlignment = Alignment.Center,
    ) {
      AnimatedVisibility(
        visible = visible,
        enter = expandVertically(
          expandFrom = Alignment.Top,
          clip = false,
        ),
        exit = shrinkVertically(
          shrinkTowards = Alignment.Top,
          clip = false,
        ),
      ) {
        LibraryPickerCard(
          currentLibrary = currentLibrary,
          libraries = libraries,
          onLibraryClick = onLibraryClick,
          onDismissRequest = dismissPopup,
        )
      }
    }
  }
}

@Composable
private fun LibraryPickerCard(
  currentLibrary: Library?,
  libraries: List<Library>,
  onLibraryClick: (Library) -> Unit,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  ElevatedCard(
    shape = MaterialTheme.shapes.extraLarge,
    elevation = CardDefaults.elevatedCardElevation(
      defaultElevation = 10.dp,
    ),
    modifier = modifier
      .fillMaxWidth()
      .padding(
        horizontal = 24.dp,
      ),
  ) {
    CenterAlignedTopAppBar(
      title = {
        Text(
          text = "Libraries",
          style = MaterialTheme.typography.titleLarge,
          fontFamily = PaytoneOneFontFamily,
        )
      },
      navigationIcon = {
        IconButton(
          onClick = onDismissRequest,
        ) {
          Icon(Icons.Rounded.Close, contentDescription = null)
        }
      },
      colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
        containerColor = Color.Transparent,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
      ),
    )

    Spacer(Modifier.height(8.dp))

    libraries.forEach { library ->
      val selected = library.id == currentLibrary?.id
      LibraryListItem(
        library = library,
        selected = selected,
        modifier = Modifier
          .clickable(
            enabled = !selected,
            onClick = { onLibraryClick(library) },
          ),
      )
    }
    Spacer(Modifier.height(16.dp))
  }
}

@Composable
private fun LibraryListItem(
  library: Library,
  selected: Boolean,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(48.dp)
      .padding(horizontal = 16.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = library.name,
      textAlign = TextAlign.Center,
      style = MaterialTheme.typography.titleLarge,
      fontFamily = PaytoneOneFontFamily,
      color = if (selected) MaterialTheme.colorScheme.secondary else LocalContentColor.current,
    )

    if (selected) {
      Icon(
        Icons.Rounded.Check,
        contentDescription = null,
        modifier = Modifier
          .align(Alignment.CenterEnd),
      )
    }
  }
}
