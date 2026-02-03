package app.campfire.whatsnew.ui.changelog

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDropUp
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.widgets.CampfireLargeTopAppBar
import app.campfire.common.compose.widgets.ErrorListState
import app.campfire.common.compose.widgets.LoadingState
import app.campfire.core.coroutines.LoadState
import app.campfire.core.di.UserScope
import app.campfire.whatsnew.api.screen.ChangelogScreen
import app.campfire.whatsnew.ui.changelog.ChangeUi.Change.Position.Bottom
import app.campfire.whatsnew.ui.changelog.ChangeUi.Change.Position.Middle
import app.campfire.whatsnew.ui.changelog.ChangeUi.Change.Position.Only
import app.campfire.whatsnew.ui.changelog.ChangeUi.Change.Position.Top
import campfire.infra.whats_new.ui.generated.resources.Res
import campfire.infra.whats_new.ui.generated.resources.changelog_title
import campfire.infra.whats_new.ui.generated.resources.error_changelog_message
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import org.jetbrains.compose.resources.stringResource

@CircuitInject(ChangelogScreen::class, UserScope::class)
@Composable
fun Changelog(
  state: ChangelogUiState,
  modifier: Modifier = Modifier,
) {
  val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
  Scaffold(
    topBar = {
      CampfireLargeTopAppBar(
        title = {
          Text(stringResource(Res.string.changelog_title))
        },
        subtitle = {
          Text(state.currentVersion)
        },
        scrollBehavior = scrollBehavior,
        navigationIcon = {
          IconButton(
            onClick = { state.eventSink(ChangelogUiEvent.Back) },
          ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
          }
        },
      )
    },
    modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
  ) { paddingValues ->
    when (val changeLogState = state.changeLogState) {
      LoadState.Error -> ErrorListState(
        message = stringResource(Res.string.error_changelog_message),
      )

      LoadState.Loading -> LoadingState(modifier = Modifier.padding(paddingValues))
      is LoadState.Loaded<out List<ChangeUi>> -> LoadedContent(
        changelog = changeLogState.data,
        onVersionClick = { version ->
          state.eventSink(ChangelogUiEvent.ToggleVersion(version))
        },
        contentPadding = paddingValues,
      )
    }
  }
}

@Composable
private fun LoadedContent(
  changelog: List<ChangeUi>,
  onVersionClick: (ChangeUi.Version) -> Unit,
  contentPadding: PaddingValues,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    contentPadding = contentPadding,
    modifier = modifier,
  ) {
    itemsIndexed(
      items = changelog,
      contentType = { _, change -> change::class },
    ) { index, change ->
      when (change) {
        is ChangeUi.Version -> VersionRow(
          version = change,
          onClick = {
            onVersionClick(change)
          },
          isFirst = index == 0,
        )

        is ChangeUi.Category -> ChangeCategoryRow(change)
        is ChangeUi.Change -> ChangeListItem(change)
      }
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun VersionRow(
  version: ChangeUi.Version,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  isFirst: Boolean = false,
) {
  Column(
    modifier = modifier,
  ) {
    if (!isFirst) {
      HorizontalDivider(
        modifier = Modifier
          .padding(vertical = 8.dp),
      )
    } else {
      Spacer(Modifier.height(8.dp))
    }
    Row(
      modifier = Modifier
        .clickable(onClick = onClick)
        .padding(
          horizontal = 16.dp,
          vertical = 12.dp,
        ),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = version.version,
        style = MaterialTheme.typography.headlineMediumEmphasized,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.weight(1f),
      )

      val arrowRotation by animateFloatAsState(
        targetValue = if (version.collapsed) 0f else 180f,
      )

      Icon(
        Icons.Rounded.ArrowDropUp,
        contentDescription = null,
        modifier = Modifier.rotate(arrowRotation),
      )
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ChangeCategoryRow(
  category: ChangeUi.Category,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .padding(
        horizontal = 16.dp,
        vertical = 8.dp,
      ),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = category.name,
      style = MaterialTheme.typography.titleMediumEmphasized,
      fontWeight = FontWeight.SemiBold,
      color = MaterialTheme.colorScheme.secondary,
    )
  }
}

private val ItemLargeCorner = 12.dp
private val ItemSmallCorner = 4.dp

private val TopShape = RoundedCornerShape(
  topStart = ItemLargeCorner,
  topEnd = ItemLargeCorner,
  bottomStart = ItemSmallCorner,
  bottomEnd = ItemSmallCorner,
)

private val BottomShape = RoundedCornerShape(
  topStart = ItemSmallCorner,
  topEnd = ItemSmallCorner,
  bottomStart = ItemLargeCorner,
  bottomEnd = ItemLargeCorner,
)

private val MiddleShape = RoundedCornerShape(ItemSmallCorner)
private val OnlyShape = RoundedCornerShape(ItemLargeCorner)

@Composable
private fun ChangeListItem(
  change: ChangeUi.Change,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier
      .padding(
        horizontal = 16.dp,
        vertical = 1.dp,
      )
      .fillMaxWidth(),
    shape = when (change.position) {
      Only -> OnlyShape
      Top -> TopShape
      Middle -> MiddleShape
      Bottom -> BottomShape
    },
    color = MaterialTheme.colorScheme.surfaceContainer,
  ) {
    Row(
      modifier = Modifier
        .padding(
          horizontal = 16.dp,
          vertical = 8.dp,
        ),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(
        text = change.text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
    }
  }
}
