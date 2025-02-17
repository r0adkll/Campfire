package app.campfire.debug.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.CampfireWindowInsets
import app.campfire.common.compose.theme.colorPalette
import app.campfire.common.compose.widgets.CampfireTopAppBar
import app.campfire.common.compose.widgets.EmptyState
import app.campfire.common.compose.widgets.LoadingState
import app.campfire.common.compose.widgets.Tag
import app.campfire.common.compose.widgets.TagGroup
import app.campfire.common.compose.widgets.TagStyle
import app.campfire.common.screens.DebugScreen
import app.campfire.core.coroutines.LoadState
import app.campfire.core.di.UserScope
import app.campfire.core.logging.LogPriority
import app.campfire.core.model.Tent
import app.campfire.debug.screen.model.EventType
import app.campfire.debug.screen.model.EventUiModel
import app.campfire.debug.theme.JetBrainsMono
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import kotlinx.collections.immutable.ImmutableList

@CircuitInject(DebugScreen::class, UserScope::class)
@Composable
fun Debug(
  state: DebugUiState,
  modifier: Modifier = Modifier,
) {
  Scaffold(
    topBar = {
      Surface(
        shadowElevation = 2.dp,
      ) {
        Column {
          CampfireTopAppBar(
            title = { Text("Events") },
            navigationIcon = {
              IconButton(
                onClick = { state.eventSink(DebugUiEvent.Back) },
              ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
              }
            },
          )
          SearchBar(
            modifier = Modifier
              .fillMaxWidth()
              .padding(
                start = 8.dp,
                end = 8.dp,
                bottom = 8.dp,
              ),
            inputField = {
              SearchBarDefaults.InputField(
                query = state.filter,
                onQueryChange = { state.eventSink(DebugUiEvent.Query(it)) },
                onSearch = { state.eventSink(DebugUiEvent.Query(it)) },
                trailingIcon = {
                  IconButton(
                    onClick = { state.eventSink(DebugUiEvent.ClearQuery) },
                  ) {
                    Icon(Icons.Rounded.Clear, contentDescription = null)
                  }
                },
                placeholder = { Text("Search") },
                expanded = false,
                onExpandedChange = {},
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
              )
            },
            expanded = false,
            onExpandedChange = {},
            windowInsets = WindowInsets(0.dp),
            content = {},
          )
        }
      }
    },
    modifier = modifier,
    contentWindowInsets = CampfireWindowInsets,
  ) { paddingValues ->
    when (state.events) {
      LoadState.Error -> EmptyState("Uh-oh! Unable to load log events")
      LoadState.Loading -> LoadingState()
      is LoadState.Loaded -> LoadedContent(state.filter, state.events.data, paddingValues)
    }
  }
}

@Composable
private fun LoadedContent(
  query: String,
  items: ImmutableList<EventUiModel>,
  contentPadding: PaddingValues,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier,
  ) {
    LazyColumn(
      contentPadding = contentPadding,
      reverseLayout = true,
      verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
      items(
        items = items,
      ) { event ->
        EventListItem(event)
      }
    }

    if (items.isEmpty()) {
      EmptyState(
        message = {
          Text(
            buildAnnotatedString {
              append("No events found for ")
              withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append("\"$query\"")
              }
            },
          )
        },
      )
    }
  }
}

private val PriorityCornerRadius = 8.dp

@Composable
private fun EventListItem(
  event: EventUiModel,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .background(MaterialTheme.colorScheme.surfaceContainerLow)
      .drawBehind {
        drawRoundRect(
          color = event.priority.color,
          topLeft = Offset(-PriorityCornerRadius.toPx(), 0f),
          size = Size(PriorityCornerRadius.toPx() * 2f, size.height),
          cornerRadius = CornerRadius(PriorityCornerRadius.toPx()),
        )

        if (event.type != EventType.None) {
          drawRect(
            color = event.type.color,
          )
        }
      },
  ) {
    Column(
      modifier = Modifier.run {
        weight(1f)
          .padding(
            horizontal = 16.dp,
            vertical = 4.dp,
          )
      },
    ) {
      Text(
        text = event.message,
        maxLines = 4,
        overflow = TextOverflow.Ellipsis,
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Medium,
        style = MaterialTheme.typography.bodySmall,
      )

      if (event.tags.isNotEmpty() || event.throwable != null) {
        Spacer(Modifier.height(4.dp))
        TagGroup(
          tags = buildList {
            if (event.throwable != null) add(Tag(event.throwable::class.simpleName!!, TagStyle.Outline))
            event.tags.map { Tag(it, TagStyle.Filled) }
          },
        )
      }
    }
  }
}

val LogPriority.color: Color get() = when (this) {
  LogPriority.VERBOSE -> Color.LightGray
  LogPriority.DEBUG -> Color.Cyan
  LogPriority.INFO -> Color.Yellow
  LogPriority.WARN -> Color.Magenta
  LogPriority.ERROR -> Color.Red
}

val EventType.color: Color get() = when (this) {
  EventType.Send -> Tent.Green.colorPalette.lightColorScheme.primaryContainer
  EventType.TrySend -> Tent.Orange.colorPalette.lightColorScheme.primaryContainer
  EventType.Receive -> Tent.Green.colorPalette.lightColorScheme.secondaryContainer
  EventType.ReceiveFailure -> Tent.Red.colorPalette.lightColorScheme.errorContainer
  EventType.None -> Color.Unspecified
}
