package app.campfire.libraries.ui.detail.composables

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.by_author_line
import campfire.features.libraries.ui.generated.resources.by_narrator_line
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import org.jetbrains.compose.resources.stringResource

private const val PLACEHOLDER_NAME = "--"

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun AuthorNarratorBar(
  authors: List<String>,
  narrators: List<String>,
  onAuthorClick: (String) -> Unit,
  onNarratorClick: (String) -> Unit,
  modifier: Modifier = Modifier,
) = SharedElementTransitionScope {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    val authorDisplayName = authors
      .ifEmpty { listOf(PLACEHOLDER_NAME) }
      .joinToString()
    PopupNameBox(
      names = authors,
      onNameClick = {
        onAuthorClick(it)
      },
      modifier = Modifier
        .align(Alignment.Top)
        .weight(1f),
    ) {
      ByLine(
        title = { Text(stringResource(Res.string.by_author_line)) },
        content = { Text(authorDisplayName) },
        modifier = Modifier
          .clickable {
            if (authors.size == 1 && authors.first() != PLACEHOLDER_NAME) {
              onAuthorClick(authors.first())
            } else {
              show()
            }
          }
          .fillMaxWidth()
          .padding(horizontal = 8.dp),
      )
    }

    VerticalDivider(
      modifier = Modifier.height(32.dp),
    )

    var isOverflowed by remember { mutableStateOf(true) }
    var isExpanded by remember { mutableStateOf(false) }
    var maxLines by remember { mutableStateOf(DefaultMaxLines) }
    val narratorsDisplayName = narrators
      .ifEmpty { listOf(PLACEHOLDER_NAME) }
      .joinToString()
    PopupNameBox(
      names = narrators,
      onNameClick = { onNarratorClick(it) },
      modifier = Modifier
        .align(Alignment.Top)
        .weight(1f),
    ) {
      ByLine(
        title = { Text(stringResource(Res.string.by_narrator_line)) },
        content = {
          Text(
            text = narratorsDisplayName,
            maxLines = if (isExpanded) Int.MAX_VALUE else maxLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { result ->
              isOverflowed = result.didOverflowHeight || isExpanded
            },
          )
        },
        modifier = Modifier
          .combinedClickable(
            onClick = {
              if (narrators.size == 1 && narrators.first() != PLACEHOLDER_NAME) {
                onNarratorClick(narrators.first())
              } else {
                show()
              }
            },
            onLongClick = {
              isExpanded = !isExpanded
            },
          )
          .fillMaxWidth()
          .padding(horizontal = 8.dp),
      )
    }
  }
}

@Composable
private fun ByLine(
  title: @Composable () -> Unit,
  content: @Composable () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    ProvideTextStyle(
      MaterialTheme.typography.labelMedium.copy(
        fontWeight = FontWeight.Bold,
      ),
    ) {
      title()
    }
    Spacer(Modifier.height(4.dp))
    ProvideTextStyle(
      MaterialTheme.typography.bodyMedium.copy(
        textAlign = TextAlign.Center,
      ),
    ) {
      content()
    }
  }
}

@Composable
private fun PopupNameBox(
  names: List<String>,
  onNameClick: (String) -> Unit,
  modifier: Modifier = Modifier,
  content: @Composable PopupNameBoxScope.() -> Unit,
) {
  Box(
    modifier = modifier,
  ) {
    val scope = remember { PopupNameBoxScope(this) }
    content(scope)

    DropdownMenu(
      expanded = scope.showPopup && names.isNotEmpty(),
      onDismissRequest = { scope.hide() },
      shape = MaterialTheme.shapes.large,
    ) {
      names.forEach { name ->
        DropdownMenuItem(
          leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
          text = { Text(name) },
          onClick = {
            onNameClick(name)
            scope.hide()
          },
        )
      }
    }
  }
}

private class PopupNameBoxScope(
  private val boxScope: BoxScope,
) : BoxScope by boxScope {

  var showPopup by mutableStateOf(false)
    private set

  fun show() {
    showPopup = true
  }

  fun hide() {
    showPopup = false
  }
}

private const val DefaultMaxLines = 2
