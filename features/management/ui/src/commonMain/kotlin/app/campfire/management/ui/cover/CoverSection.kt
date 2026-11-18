// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.management.ui.cover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Delete
import app.campfire.common.compose.icons.rounded.Folder
import app.campfire.common.compose.icons.rounded.Globe
import app.campfire.common.compose.icons.rounded.Search
import app.campfire.common.compose.widgets.CoverImage
import app.campfire.common.compose.widgets.coverImageShape
import app.campfire.common.compose.widgets.placeholderBookPainter
import app.campfire.core.extensions.asReadableBytes
import app.campfire.libraries.api.CoverUpdate
import app.campfire.management.api.cover.BookImage
import app.campfire.management.api.match.ImageSize
import app.campfire.management.ui.editor.StagedCover
import app.campfire.management.ui.editor.StagedCoverChange
import campfire.features.management.ui.generated.resources.Res
import campfire.features.management.ui.generated.resources.action_search
import campfire.features.management.ui.generated.resources.cd_cover_result
import campfire.features.management.ui.generated.resources.cd_current_cover
import campfire.features.management.ui.generated.resources.cover_address_hint
import campfire.features.management.ui.generated.resources.cover_address_label
import campfire.features.management.ui.generated.resources.cover_address_preview
import campfire.features.management.ui.generated.resources.cover_current
import campfire.features.management.ui.generated.resources.cover_file_chosen
import campfire.features.management.ui.generated.resources.cover_file_in_use
import campfire.features.management.ui.generated.resources.cover_file_use
import campfire.features.management.ui.generated.resources.cover_files_failed
import campfire.features.management.ui.generated.resources.cover_files_none
import campfire.features.management.ui.generated.resources.cover_files_retry
import campfire.features.management.ui.generated.resources.cover_low_resolution
import campfire.features.management.ui.generated.resources.cover_mode_address
import campfire.features.management.ui.generated.resources.cover_mode_files
import campfire.features.management.ui.generated.resources.cover_mode_files_count
import campfire.features.management.ui.generated.resources.cover_mode_search
import campfire.features.management.ui.generated.resources.cover_none
import campfire.features.management.ui.generated.resources.cover_remove
import campfire.features.management.ui.generated.resources.cover_results_failed
import campfire.features.management.ui.generated.resources.cover_results_none
import campfire.features.management.ui.generated.resources.cover_size_square
import campfire.features.management.ui.generated.resources.cover_source_best
import campfire.features.management.ui.generated.resources.cover_title
import campfire.features.management.ui.generated.resources.cover_upload_needed
import campfire.features.management.ui.generated.resources.match_author
import campfire.features.management.ui.generated.resources.match_source
import campfire.features.management.ui.generated.resources.review_cover_size
import org.jetbrains.compose.resources.stringResource

/**
 * The editor's Cover section: the book's cover, or the one chosen to take its place, then where to
 * find a new one: the server's sources, an image's address, or the book's own images. Where it's
 * [wide], they're all out at once, with the cover and the ways to it beside the sources' results;
 * otherwise one at a time, under the cover.
 */
@Composable
internal fun CoverSection(
  state: CoverUiState,
  enabled: Boolean,
  modifier: Modifier = Modifier,
  wide: Boolean = false,
) {
  CoverPreviewSheet(state = state, enabled = enabled)
  if (wide) {
    WideCoverSection(state = state, enabled = enabled, modifier = modifier)
  } else {
    CompactCoverSection(state = state, enabled = enabled, modifier = modifier)
  }
}

@Composable
private fun CompactCoverSection(
  state: CoverUiState,
  enabled: Boolean,
  modifier: Modifier = Modifier,
) {
  // One grid, so the cover and the search scroll away with the results in a short window
  LazyVerticalGrid(
    columns = GridCells.Adaptive(ResultMinSize),
    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 24.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
    modifier = modifier
      .imePadding()
      .testTag("cover_section"),
  ) {
    wholeRow("cover") {
      CurrentCover(state = state, enabled = enabled)
    }
    wholeRow("modes") {
      CoverModes(state = state, enabled = enabled, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
    }
    when (state.mode) {
      CoverMode.Search -> {
        wholeRow("search") { SearchFields(state = state, enabled = enabled) }
        resultItems(state, enabled)
      }
      CoverMode.Address -> wholeRow("address") { AddressEntry(state = state, enabled = enabled) }
      CoverMode.BookFiles -> wholeRow("files") { BookImagesList(state = state, enabled = enabled) }
    }
  }
}

/**
 * The cover and the ways to a new one but searching down the leading side, and the search with
 * what each source found beside them.
 */
@Composable
private fun WideCoverSection(
  state: CoverUiState,
  enabled: Boolean,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .fillMaxSize()
      .imePadding()
      .testTag("cover_section"),
  ) {
    Column(
      verticalArrangement = Arrangement.spacedBy(12.dp),
      modifier = Modifier
        .width(CoverPanelWidth)
        .fillMaxHeight()
        .verticalScroll(rememberScrollState())
        .padding(24.dp),
    ) {
      CurrentCover(state = state, enabled = enabled, large = true)
      if (state.canRemove && state.currentUrl != null) {
        RemoveCoverButton(state = state, enabled = enabled)
      }

      if (state.canUseAddresses) {
        PanelHeader(stringResource(Res.string.cover_mode_address))
        AddressEntry(state = state, enabled = enabled)
      }

      val images = (state.bookImages as? BookImages.Loaded)?.images
      PanelHeader(
        if (images != null) {
          stringResource(Res.string.cover_mode_files_count, images.size)
        } else {
          stringResource(Res.string.cover_mode_files)
        },
      )
      BookImagesList(state = state, enabled = enabled)
    }

    VerticalDivider()

    LazyVerticalGrid(
      columns = GridCells.Adaptive(WideResultMinSize),
      contentPadding = PaddingValues(24.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      modifier = Modifier
        .weight(1f)
        .fillMaxHeight()
        .testTag("cover_results"),
    ) {
      if (state.canUseAddresses) {
        wholeRow("search") { SearchFields(state = state, enabled = enabled, inRow = true) }
        resultItems(state, enabled)
      } else {
        wholeRow("upload_needed") {
          Text(
            text = stringResource(Res.string.cover_upload_needed),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@Composable
private fun PanelHeader(
  text: String,
  modifier: Modifier = Modifier,
) {
  Text(
    text = text,
    style = MaterialTheme.typography.titleSmall,
    color = MaterialTheme.colorScheme.primary,
    modifier = modifier.padding(top = 12.dp),
  )
}

@Composable
private fun RemoveCoverButton(
  state: CoverUiState,
  enabled: Boolean,
  modifier: Modifier = Modifier,
) {
  TextButton(
    onClick = { state.eventSink(CoverUiEvent.Remove) },
    enabled = enabled && state.staged?.update != CoverUpdate.Remove,
    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
    modifier = modifier.testTag("cover_remove"),
  ) {
    Icon(CampfireIcons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
    Text(stringResource(Res.string.cover_remove))
  }
}

private fun LazyGridScope.wholeRow(
  key: String,
  content: @Composable LazyGridItemScope.() -> Unit,
) = item(key = key, span = { GridItemSpan(maxLineSpan) }, content = content)

@Composable
private fun CurrentCover(
  state: CoverUiState,
  enabled: Boolean,
  modifier: Modifier = Modifier,
  large: Boolean = false,
) {
  val staged = state.staged
  if (staged != null) {
    StagedCoverChange(
      staged = staged,
      currentUrl = state.currentUrl,
      enabled = enabled,
      onKeepCurrent = { state.eventSink(CoverUiEvent.KeepCurrent) },
      modifier = modifier,
    )
    return
  }

  if (large) {
    Column(
      verticalArrangement = Arrangement.spacedBy(12.dp),
      modifier = modifier.testTag("cover_current"),
    ) {
      CoverImage(
        imageUrl = state.currentUrl,
        contentDescription = stringResource(Res.string.cd_current_cover),
        size = LargeCoverSize,
        placeholder = placeholderBookPainter(),
        shape = coverImageShape(LargeCoverSize),
        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
      )
      CurrentCoverInfo(state)
    }
    return
  }

  Row(
    horizontalArrangement = Arrangement.spacedBy(16.dp),
    modifier = modifier.testTag("cover_current"),
  ) {
    CoverImage(
      imageUrl = state.currentUrl,
      contentDescription = stringResource(Res.string.cd_current_cover),
      size = CurrentCoverSize,
      placeholder = placeholderBookPainter(),
      shape = coverImageShape(CurrentCoverSize),
      modifier = Modifier.size(CurrentCoverSize),
    )
    CurrentCoverInfo(state, Modifier.padding(top = 4.dp))
  }
}

/** Whether the book has a cover, and how big it is once its file says */
@Composable
private fun CurrentCoverInfo(
  state: CoverUiState,
  modifier: Modifier = Modifier,
) {
  Column(
    verticalArrangement = Arrangement.spacedBy(6.dp),
    modifier = modifier,
  ) {
    Text(
      text = stringResource(if (state.currentUrl != null) Res.string.cover_current else Res.string.cover_none),
      style = MaterialTheme.typography.titleSmall,
    )
    val size = state.currentSize
    if (size != null) {
      Text(
        text = stringResource(Res.string.review_cover_size, size.width, size.height),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      if (minOf(size.width, size.height) < LowResolution) {
        Tag(
          text = stringResource(Res.string.cover_low_resolution),
          color = MaterialTheme.colorScheme.errorContainer,
          contentColor = MaterialTheme.colorScheme.onErrorContainer,
        )
      }
    }
  }
}

@Composable
private fun CoverModes(
  state: CoverUiState,
  enabled: Boolean,
  modifier: Modifier = Modifier,
) {
  Column(
    verticalArrangement = Arrangement.spacedBy(4.dp),
    modifier = modifier,
  ) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      ModeChip(
        mode = CoverMode.Search,
        state = state,
        enabled = enabled && state.canUseAddresses,
        label = stringResource(Res.string.cover_mode_search),
        icon = { Icon(CampfireIcons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
      )
      ModeChip(
        mode = CoverMode.Address,
        state = state,
        enabled = enabled && state.canUseAddresses,
        label = stringResource(Res.string.cover_mode_address),
        icon = { Icon(CampfireIcons.Rounded.Globe, contentDescription = null, modifier = Modifier.size(18.dp)) },
      )
      val images = (state.bookImages as? BookImages.Loaded)?.images
      ModeChip(
        mode = CoverMode.BookFiles,
        state = state,
        enabled = enabled,
        label = if (images != null) {
          stringResource(Res.string.cover_mode_files_count, images.size)
        } else {
          stringResource(Res.string.cover_mode_files)
        },
        icon = { Icon(CampfireIcons.Rounded.Folder, contentDescription = null, modifier = Modifier.size(18.dp)) },
      )
      // Only when there's a cover to take away, and the account may
      if (state.canRemove && state.currentUrl != null) {
        AssistChip(
          onClick = { state.eventSink(CoverUiEvent.Remove) },
          enabled = enabled && state.staged?.update != CoverUpdate.Remove,
          label = { Text(stringResource(Res.string.cover_remove)) },
          leadingIcon = {
            Icon(CampfireIcons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
          },
          colors = AssistChipDefaults.assistChipColors(
            labelColor = MaterialTheme.colorScheme.error,
            leadingIconContentColor = MaterialTheme.colorScheme.error,
          ),
          modifier = Modifier.testTag("cover_remove"),
        )
      }
    }
    if (!state.canUseAddresses) {
      Text(
        text = stringResource(Res.string.cover_upload_needed),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun ModeChip(
  mode: CoverMode,
  state: CoverUiState,
  enabled: Boolean,
  label: String,
  icon: @Composable () -> Unit,
  modifier: Modifier = Modifier,
) {
  FilterChip(
    selected = state.mode == mode,
    onClick = { state.eventSink(CoverUiEvent.ChooseMode(mode)) },
    enabled = enabled,
    label = { Text(label) },
    leadingIcon = icon,
    modifier = modifier.testTag("cover_mode_${mode.name.lowercase()}"),
  )
}

private fun LazyGridScope.resultItems(
  state: CoverUiState,
  enabled: Boolean,
) {
  state.results.forEach { group ->
    wholeRow("results_${group.provider.id}") {
      ResultsHeader(group = group, modifier = Modifier.padding(top = 12.dp))
    }
    val covers = (group.search as? CoverSearch.Found)?.covers.orEmpty()
    items(covers, key = { "${group.provider.id}:$it" }) { url ->
      CoverResult(
        url = url,
        size = state.sizes[url],
        contentDescription = stringResource(Res.string.cd_cover_result, covers.indexOf(url) + 1, group.provider.name),
        enabled = enabled,
        onClick = { state.eventSink(CoverUiEvent.Preview(url)) },
      )
    }
  }
}

/** Where to search and for what, one field under another, or side by side when [inRow] */
@Composable
private fun SearchFields(
  state: CoverUiState,
  enabled: Boolean,
  modifier: Modifier = Modifier,
  inRow: Boolean = false,
) {
  if (inRow) {
    // All in one row where the title and author still have the room to read, or in two
    BoxWithConstraints(modifier) {
      val oneRow = maxWidth >= OneSearchRowWidth
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          SourceField(state, enabled, Modifier.width(SourceFieldWidth))
          SearchTitleField(state, enabled, Modifier.weight(1f))
          if (oneRow) {
            SearchAuthorField(state, enabled, Modifier.weight(1f))
            SearchButton(state, enabled)
          }
        }
        if (!oneRow) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            SearchAuthorField(state, enabled, Modifier.weight(1f))
            SearchButton(state, enabled)
          }
        }
      }
    }
    return
  }

  Column(
    verticalArrangement = Arrangement.spacedBy(8.dp),
    modifier = modifier,
  ) {
    SourceField(state, enabled, Modifier.fillMaxWidth())
    SearchTitleField(state, enabled, Modifier.fillMaxWidth())
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      SearchAuthorField(state, enabled, Modifier.weight(1f))
      SearchButton(state, enabled)
    }
  }
}

@Composable
private fun SourceField(
  state: CoverUiState,
  enabled: Boolean,
  modifier: Modifier = Modifier,
) {
  var choosing by remember { mutableStateOf(false) }
  ExposedDropdownMenuBox(
    expanded = choosing,
    onExpandedChange = { choosing = it && state.sources.isNotEmpty() },
    modifier = modifier,
  ) {
    OutlinedTextField(
      value = state.source?.let { sourceName(it) }.orEmpty(),
      onValueChange = {},
      readOnly = true,
      enabled = enabled,
      singleLine = true,
      label = { Text(stringResource(Res.string.match_source)) },
      trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = choosing) },
      modifier = Modifier
        .fillMaxWidth()
        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled)
        .testTag("cover_source"),
    )
    ExposedDropdownMenu(
      expanded = choosing,
      onDismissRequest = { choosing = false },
    ) {
      state.sources.forEach { source ->
        DropdownMenuItem(
          text = { Text(sourceName(source)) },
          onClick = {
            choosing = false
            state.eventSink(CoverUiEvent.ChooseSource(source))
          },
          contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
        )
      }
    }
  }
}

@Composable
private fun SearchTitleField(
  state: CoverUiState,
  enabled: Boolean,
  modifier: Modifier = Modifier,
) {
  OutlinedTextField(
    state = state.title,
    enabled = enabled,
    label = { Text(stringResource(Res.string.cover_title)) },
    lineLimits = TextFieldLineLimits.SingleLine,
    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    onKeyboardAction = { state.eventSink(CoverUiEvent.Search) },
    modifier = modifier.testTag("cover_search_title"),
  )
}

@Composable
private fun SearchAuthorField(
  state: CoverUiState,
  enabled: Boolean,
  modifier: Modifier = Modifier,
) {
  OutlinedTextField(
    state = state.author,
    enabled = enabled,
    label = { Text(stringResource(Res.string.match_author)) },
    lineLimits = TextFieldLineLimits.SingleLine,
    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    onKeyboardAction = { state.eventSink(CoverUiEvent.Search) },
    modifier = modifier.testTag("cover_search_author"),
  )
}

@Composable
private fun SearchButton(
  state: CoverUiState,
  enabled: Boolean,
  modifier: Modifier = Modifier,
) {
  val searching = state.results.any { it.search == CoverSearch.Searching }
  Button(
    onClick = { state.eventSink(CoverUiEvent.Search) },
    enabled = enabled && !searching && state.sources.isNotEmpty(),
    modifier = modifier.padding(top = 8.dp).testTag("cover_search"),
  ) {
    Icon(CampfireIcons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp))
    Text(stringResource(Res.string.action_search), modifier = Modifier.padding(start = 8.dp))
  }
}

@Composable
private fun sourceName(source: CoverSource): String = when (source) {
  CoverSource.Best -> stringResource(Res.string.cover_source_best)
  is CoverSource.Single -> source.provider.name
}

@Composable
private fun ResultsHeader(
  group: CoverResults,
  modifier: Modifier = Modifier,
) {
  Row(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
      .fillMaxWidth()
      .testTag("cover_results_header"),
  ) {
    Text(
      text = group.provider.name,
      style = MaterialTheme.typography.titleSmall,
      modifier = Modifier.weight(1f),
    )
    when (val search = group.search) {
      CoverSearch.Searching -> CircularProgressIndicator(
        strokeWidth = 2.dp,
        modifier = Modifier.size(16.dp),
      )
      is CoverSearch.Found -> Text(
        text = if (search.covers.isEmpty()) {
          stringResource(Res.string.cover_results_none)
        } else {
          search.covers.size.toString()
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      CoverSearch.Failed -> Text(
        text = stringResource(Res.string.cover_results_failed),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
      )
    }
  }
}

/** A cover a source found, with how big it is once its file says */
@Composable
private fun CoverResult(
  url: String,
  size: ImageSize?,
  contentDescription: String,
  enabled: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val shape = coverImageShape(ResultMinSize)
  Box(
    modifier = modifier
      .aspectRatio(1f)
      .clip(shape)
      .clickable(enabled = enabled, onClick = onClick)
      .testTag("cover_result"),
  ) {
    CoverImage(
      imageUrl = url,
      contentDescription = contentDescription,
      size = ResultMinSize,
      placeholder = placeholderBookPainter(),
      shape = shape,
      modifier = Modifier.fillMaxSize(),
    )
    if (size != null) {
      Text(
        text = sizeLabel(size),
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
        maxLines = 1,
        modifier = Modifier
          .align(Alignment.BottomStart)
          .padding(6.dp)
          .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
          .padding(horizontal = 5.dp, vertical = 1.dp),
      )
    }
  }
}

@Composable
private fun sizeLabel(size: ImageSize): String = if (size.width == size.height) {
  stringResource(Res.string.cover_size_square, size.width)
} else {
  stringResource(Res.string.review_cover_size, size.width, size.height)
}

@Composable
private fun AddressEntry(
  state: CoverUiState,
  enabled: Boolean,
  modifier: Modifier = Modifier,
) {
  val preview = { state.eventSink(CoverUiEvent.PreviewAddress) }
  val address = state.address.text.toString().trim()
  Column(
    verticalArrangement = Arrangement.spacedBy(8.dp),
    modifier = modifier,
  ) {
    OutlinedTextField(
      state = state.address,
      enabled = enabled,
      label = { Text(stringResource(Res.string.cover_address_label)) },
      lineLimits = TextFieldLineLimits.SingleLine,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
      onKeyboardAction = { preview() },
      modifier = Modifier.fillMaxWidth().testTag("cover_address"),
    )
    Text(
      text = stringResource(Res.string.cover_address_hint),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Button(
      onClick = preview,
      enabled = enabled && (address.startsWith("https://") || address.startsWith("http://")),
      modifier = Modifier
        .align(Alignment.End)
        .testTag("cover_address_preview"),
    ) {
      Text(stringResource(Res.string.cover_address_preview))
    }
  }
}

/** The images among the book's own files, each to use as its cover */
@Composable
private fun BookImagesList(
  state: CoverUiState,
  enabled: Boolean,
  modifier: Modifier = Modifier,
) {
  Column(
    verticalArrangement = Arrangement.spacedBy(8.dp),
    modifier = modifier.fillMaxWidth(),
  ) {
    when (val images = state.bookImages) {
      BookImages.Loading -> LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 8.dp))

      BookImages.Failed -> {
        Text(
          text = stringResource(Res.string.cover_files_failed),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(
          onClick = { state.eventSink(CoverUiEvent.RetryBookImages) },
          modifier = Modifier.testTag("cover_files_retry"),
        ) {
          Text(stringResource(Res.string.cover_files_retry))
        }
      }

      is BookImages.Loaded -> if (images.images.isEmpty()) {
        Text(
          text = stringResource(Res.string.cover_files_none),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      } else {
        images.images.forEach { image ->
          key(image.path) {
            BookImageRow(
              image = image,
              staged = state.staged,
              enabled = enabled,
              onUse = { state.eventSink(CoverUiEvent.UseBookImage(image)) },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun BookImageRow(
  image: BookImage,
  staged: StagedCover?,
  enabled: Boolean,
  onUse: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    modifier = modifier.testTag("cover_file"),
  ) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
      CoverImage(
        imageUrl = image.url,
        contentDescription = null,
        size = FileThumbnailSize,
        placeholder = placeholderBookPainter(),
        shape = coverImageShape(FileThumbnailSize),
        modifier = Modifier.size(FileThumbnailSize),
      )
      Column(Modifier.weight(1f)) {
        Text(
          text = image.name,
          style = MaterialTheme.typography.bodyMedium,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = image.sizeBytes.asReadableBytes(),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      val chosen = (staged?.update as? CoverUpdate.FromServerFile)?.path == image.path
      when {
        chosen -> Tag(
          text = stringResource(Res.string.cover_file_chosen),
          color = MaterialTheme.colorScheme.primaryContainer,
          contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        image.isCover && staged == null -> Tag(
          text = stringResource(Res.string.cover_file_in_use),
          color = MaterialTheme.colorScheme.secondaryContainer,
          contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        else -> TextButton(onClick = onUse, enabled = enabled) {
          Text(stringResource(Res.string.cover_file_use))
        }
      }
    }
  }
}

@Composable
private fun Tag(
  text: String,
  color: Color,
  contentColor: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = MaterialTheme.shapes.small,
    color = color,
    contentColor = contentColor,
    modifier = modifier,
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelMedium,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
    )
  }
}

private val CurrentCoverSize = 140.dp
private val ResultMinSize = 104.dp

/** The cover and the ways to a new one, beside the search where there's the room */
private val CoverPanelWidth = 352.dp
private val LargeCoverSize = 304.dp
private val WideResultMinSize = 128.dp
private val SourceFieldWidth = 200.dp
private val OneSearchRowWidth = 760.dp
private val FileThumbnailSize = 56.dp

/** Pixels along a cover's shorter side below which it looks soft on a big screen */
private const val LowResolution = 800
