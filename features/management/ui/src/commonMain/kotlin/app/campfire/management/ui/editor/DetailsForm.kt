// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.management.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Add
import app.campfire.common.compose.icons.rounded.Close
import app.campfire.common.compose.widgets.IconButtonTooltip
import campfire.features.management.ui.generated.resources.Res
import campfire.features.management.ui.generated.resources.action_add_series
import campfire.features.management.ui.generated.resources.action_keep_mine
import campfire.features.management.ui.generated.resources.action_reload
import campfire.features.management.ui.generated.resources.banner_behind_server
import campfire.features.management.ui.generated.resources.banner_cant_edit
import campfire.features.management.ui.generated.resources.banner_filled_from
import campfire.features.management.ui.generated.resources.cd_changed
import campfire.features.management.ui.generated.resources.cd_dismiss
import campfire.features.management.ui.generated.resources.cd_remove_series
import campfire.features.management.ui.generated.resources.editor_footer
import campfire.features.management.ui.generated.resources.error_series_duplicate
import campfire.features.management.ui.generated.resources.error_series_name_missing
import campfire.features.management.ui.generated.resources.error_title_required
import campfire.features.management.ui.generated.resources.error_year_invalid
import campfire.features.management.ui.generated.resources.label_abridged
import campfire.features.management.ui.generated.resources.label_asin
import campfire.features.management.ui.generated.resources.label_authors
import campfire.features.management.ui.generated.resources.label_description
import campfire.features.management.ui.generated.resources.label_explicit
import campfire.features.management.ui.generated.resources.label_genres
import campfire.features.management.ui.generated.resources.label_isbn
import campfire.features.management.ui.generated.resources.label_language
import campfire.features.management.ui.generated.resources.label_narrators
import campfire.features.management.ui.generated.resources.label_published_year
import campfire.features.management.ui.generated.resources.label_publisher
import campfire.features.management.ui.generated.resources.label_series_name
import campfire.features.management.ui.generated.resources.label_series_sequence
import campfire.features.management.ui.generated.resources.label_subtitle
import campfire.features.management.ui.generated.resources.label_tags
import campfire.features.management.ui.generated.resources.label_title
import campfire.features.management.ui.generated.resources.placeholder_optional
import campfire.features.management.ui.generated.resources.section_classification
import campfire.features.management.ui.generated.resources.section_identifiers
import campfire.features.management.ui.generated.resources.section_people
import campfire.features.management.ui.generated.resources.section_publishing
import campfire.features.management.ui.generated.resources.section_series
import campfire.features.management.ui.generated.resources.warning_explicit_hidden
import com.mohamedrejeb.richeditor.ui.material3.OutlinedRichTextEditor
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** How the form lays its fields out for the room it has */
internal enum class FormLayout {
  /** One field under the other, in groups under headers */
  SingleColumn,

  /** What the book is on the leading side, how it was published and identified on the trailing one */
  TwoColumns,

  /** Rows of fields that go together side by side, without headers, as a desktop lays them out */
  Dense,
}

/** The book's details as fields, laid out as [layout] has them */
@Composable
internal fun DetailsForm(
  draft: BookDetailsDraft,
  enabled: Boolean,
  canEdit: Boolean,
  canSeeExplicit: Boolean,
  suggestions: DetailSuggestions,
  isBehindServer: Boolean,
  filledFrom: String?,
  /** The book's own cover, if it has one, to show beside a new one */
  currentCoverUrl: String?,
  onReload: () -> Unit,
  onKeepMine: () -> Unit,
  onDismissFilledFrom: () -> Unit,
  layout: FormLayout,
  inlineSuggestions: Boolean,
  modifier: Modifier = Modifier,
) = CompositionLocalProvider(LocalInlineSuggestions provides inlineSuggestions) {
  val focusManager = LocalFocusManager.current
  Box(
    contentAlignment = Alignment.TopCenter,
    modifier = modifier
      .fillMaxSize()
      // Before scrolling, so the keyboard shrinks what scrolls and a focused field scrolls into view
      .imePadding()
      .verticalScroll(rememberScrollState()),
  ) {
    Column(
      verticalArrangement = Arrangement.spacedBy(if (layout == FormLayout.Dense) FieldSpacing else GroupSpacing),
      modifier = Modifier
        .widthIn(max = MaxFormWidth)
        .fillMaxWidth()
        .padding(if (layout == FormLayout.Dense) 24.dp else 16.dp),
    ) {
      if (!canEdit) {
        Banner(text = stringResource(Res.string.banner_cant_edit))
      }
      if (isBehindServer) {
        Banner(text = stringResource(Res.string.banner_behind_server)) {
          TextButton(onClick = onKeepMine, modifier = Modifier.testTag("editor_keep_mine")) {
            Text(stringResource(Res.string.action_keep_mine))
          }
          TextButton(
            onClick = {
              // The fields are about to be replaced, so the keyboard lets go of the one it's in
              focusManager.clearFocus()
              onReload()
            },
            modifier = Modifier.testTag("editor_reload"),
          ) {
            Text(stringResource(Res.string.action_reload))
          }
        }
      }

      if (filledFrom != null) {
        Banner(text = stringResource(Res.string.banner_filled_from, filledFrom)) {
          TextButton(onClick = onDismissFilledFrom, modifier = Modifier.testTag("editor_dismiss_filled")) {
            Text(stringResource(Res.string.cd_dismiss))
          }
        }
      }
      draft.newCover?.let { staged ->
        StagedCoverChange(
          staged = staged,
          currentUrl = currentCoverUrl,
          enabled = enabled,
          onKeepCurrent = { draft.newCover = null },
        )
      }

      when (layout) {
        FormLayout.Dense -> DenseFields(draft, suggestions, enabled, canSeeExplicit)

        FormLayout.TwoColumns -> Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
          Column(
            verticalArrangement = Arrangement.spacedBy(GroupSpacing),
            modifier = Modifier.weight(1f),
          ) {
            IdentityFields(draft, enabled)
            PeopleFields(draft, suggestions, enabled)
            SeriesFields(draft, suggestions, enabled)
            DescriptionField(draft, enabled)
          }
          Column(
            verticalArrangement = Arrangement.spacedBy(GroupSpacing),
            modifier = Modifier.weight(1f),
          ) {
            PublishingFields(draft, suggestions, enabled)
            ClassificationFields(draft, suggestions, enabled)
            IdentifierFields(draft, enabled)
            FlagFields(draft, enabled, canSeeExplicit)
          }
        }

        FormLayout.SingleColumn -> {
          IdentityFields(draft, enabled)
          PeopleFields(draft, suggestions, enabled)
          SeriesFields(draft, suggestions, enabled)
          DescriptionField(draft, enabled)
          PublishingFields(draft, suggestions, enabled)
          ClassificationFields(draft, suggestions, enabled)
          IdentifierFields(draft, enabled)
          FlagFields(draft, enabled, canSeeExplicit)
        }
      }

      Text(
        text = stringResource(Res.string.editor_footer),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

/** Every field, with those that go together side by side and no headers between them */
@Composable
private fun DenseFields(
  draft: BookDetailsDraft,
  suggestions: DetailSuggestions,
  enabled: Boolean,
  canSeeExplicit: Boolean,
) {
  Row(horizontalArrangement = Arrangement.spacedBy(FieldSpacing)) {
    TitleField(draft, enabled, Modifier.weight(2f))
    SubtitleField(draft, enabled, Modifier.weight(1f))
  }
  Row(horizontalArrangement = Arrangement.spacedBy(FieldSpacing)) {
    AuthorsField(draft, suggestions, enabled, Modifier.weight(1f))
    NarratorsField(draft, suggestions, enabled, Modifier.weight(1f))
  }
  SeriesFields(draft, suggestions, enabled, showHeader = false)
  DescriptionField(draft, enabled)
  Row(horizontalArrangement = Arrangement.spacedBy(FieldSpacing)) {
    PublisherField(draft, suggestions, enabled, Modifier.weight(1f))
    PublishedYearField(draft, enabled, Modifier.width(YearFieldWidth))
    LanguageField(draft, suggestions, enabled, Modifier.width(LanguageFieldWidth))
  }
  Row(horizontalArrangement = Arrangement.spacedBy(FieldSpacing)) {
    GenresField(draft, suggestions, enabled, Modifier.weight(1f))
    TagsField(draft, suggestions, enabled, Modifier.weight(1f))
  }
  Row(horizontalArrangement = Arrangement.spacedBy(FieldSpacing)) {
    IsbnField(draft, enabled, Modifier.weight(1f))
    AsinField(draft, enabled, Modifier.weight(1f))
  }
  Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
    ExplicitField(draft, enabled, canSeeExplicit, Modifier.weight(1f))
    AbridgedField(draft, enabled, Modifier.weight(1f))
  }
}

@Composable
private fun IdentityFields(
  draft: BookDetailsDraft,
  enabled: Boolean,
  modifier: Modifier = Modifier,
) {
  Column(
    verticalArrangement = Arrangement.spacedBy(FieldSpacing),
    modifier = modifier,
  ) {
    TitleField(draft, enabled, Modifier.fillMaxWidth())
    SubtitleField(draft, enabled, Modifier.fillMaxWidth())
  }
}

@Composable
private fun PeopleFields(
  draft: BookDetailsDraft,
  suggestions: DetailSuggestions,
  enabled: Boolean,
  modifier: Modifier = Modifier,
) {
  Column(
    verticalArrangement = Arrangement.spacedBy(FieldSpacing),
    modifier = modifier,
  ) {
    SectionHeader(Res.string.section_people)
    AuthorsField(draft, suggestions, enabled, Modifier.fillMaxWidth())
    NarratorsField(draft, suggestions, enabled, Modifier.fillMaxWidth())
  }
}

@Composable
private fun SeriesFields(
  draft: BookDetailsDraft,
  suggestions: DetailSuggestions,
  enabled: Boolean,
  modifier: Modifier = Modifier,
  showHeader: Boolean = true,
) {
  // A row that was just added gets the focus, to type its name
  var added by remember { mutableStateOf<SeriesRowState?>(null) }
  val changed = draft.isChanged(BookDetail.Series)

  Column(
    verticalArrangement = Arrangement.spacedBy(FieldSpacing),
    modifier = modifier,
  ) {
    if (showHeader) {
      SectionHeader(Res.string.section_series, changed = changed)
    }
    draft.series.forEachIndexed { index, row ->
      key(row) {
        SeriesRow(
          row = row,
          index = index,
          error = draft.seriesErrorAt(index),
          suggestions = suggestions.series,
          // The series the other rows name
          excluding = draft.series.filter { it !== row }.map { it.name.text.toString().trim() },
          enabled = enabled,
          // Without a header to mark, the first row's label says the series changed
          changed = !showHeader && index == 0 && changed,
          focusOnStart = row === added,
          onRemove = { draft.removeSeries(row) },
        )
      }
    }
    TextButton(
      onClick = {
        draft.addSeries()
        added = draft.series.last()
      },
      enabled = enabled,
      modifier = Modifier.testTag("series_add"),
    ) {
      Icon(
        imageVector = CampfireIcons.Rounded.Add,
        contentDescription = null,
        modifier = Modifier.size(ButtonDefaults.IconSize),
      )
      Spacer(Modifier.width(ButtonDefaults.IconSpacing))
      Text(stringResource(Res.string.action_add_series))
    }
  }
}

@Composable
private fun SeriesRow(
  row: SeriesRowState,
  index: Int,
  error: SeriesRowError?,
  suggestions: List<String>,
  excluding: List<String>,
  enabled: Boolean,
  changed: Boolean,
  focusOnStart: Boolean,
  onRemove: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val focusRequester = remember { FocusRequester() }
  LaunchedEffect(Unit) {
    if (focusOnStart) focusRequester.requestFocus()
  }

  Row(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    modifier = modifier,
  ) {
    SuggestingTextField(
      state = row.name,
      suggestions = suggestions,
      excluding = excluding,
      enabled = enabled,
      modifier = Modifier.weight(1f),
    ) { anchor ->
      TextDetailField(
        state = row.name,
        label = Res.string.label_series_name,
        changed = changed,
        enabled = enabled,
        error = when (error) {
          SeriesRowError.MissingName -> Res.string.error_series_name_missing
          SeriesRowError.Duplicate -> Res.string.error_series_duplicate
          null -> null
        },
        capitalization = KeyboardCapitalization.Words,
        modifier = anchor
          .fillMaxWidth()
          .focusRequester(focusRequester)
          .testTag("series_name_$index"),
      )
    }
    TextDetailField(
      state = row.sequence,
      label = Res.string.label_series_sequence,
      changed = false,
      enabled = enabled,
      keyboardType = KeyboardType.Ascii,
      modifier = Modifier
        .width(SequenceFieldWidth)
        .testTag("series_sequence_$index"),
    )
    val removeLabel = stringResource(Res.string.cd_remove_series)
    IconButtonTooltip(
      text = removeLabel,
      modifier = Modifier.padding(top = RemoveButtonTopPadding),
    ) {
      IconButton(
        onClick = onRemove,
        enabled = enabled,
        modifier = Modifier.testTag("series_remove_$index"),
      ) {
        Icon(CampfireIcons.Rounded.Close, contentDescription = removeLabel)
      }
    }
  }
}

@Composable
private fun DescriptionField(
  draft: BookDetailsDraft,
  enabled: Boolean,
  modifier: Modifier = Modifier,
) {
  OutlinedRichTextEditor(
    state = draft.description,
    enabled = enabled,
    label = {
      FieldLabel(
        text = stringResource(Res.string.label_description),
        changed = draft.isChanged(BookDetail.Description),
      )
    },
    minLines = 4,
    modifier = modifier
      .fillMaxWidth()
      .testTag("field_description"),
  )
}

@Composable
private fun PublishingFields(
  draft: BookDetailsDraft,
  suggestions: DetailSuggestions,
  enabled: Boolean,
  modifier: Modifier = Modifier,
) {
  Column(
    verticalArrangement = Arrangement.spacedBy(FieldSpacing),
    modifier = modifier,
  ) {
    SectionHeader(Res.string.section_publishing)
    PublisherField(draft, suggestions, enabled, Modifier.fillMaxWidth())
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      PublishedYearField(draft, enabled, Modifier.width(YearFieldWidth))
      LanguageField(draft, suggestions, enabled, Modifier.weight(1f))
    }
  }
}

@Composable
private fun ClassificationFields(
  draft: BookDetailsDraft,
  suggestions: DetailSuggestions,
  enabled: Boolean,
  modifier: Modifier = Modifier,
) {
  Column(
    verticalArrangement = Arrangement.spacedBy(FieldSpacing),
    modifier = modifier,
  ) {
    SectionHeader(Res.string.section_classification)
    GenresField(draft, suggestions, enabled, Modifier.fillMaxWidth())
    TagsField(draft, suggestions, enabled, Modifier.fillMaxWidth())
  }
}

@Composable
private fun IdentifierFields(
  draft: BookDetailsDraft,
  enabled: Boolean,
  modifier: Modifier = Modifier,
) {
  Column(
    verticalArrangement = Arrangement.spacedBy(FieldSpacing),
    modifier = modifier,
  ) {
    SectionHeader(Res.string.section_identifiers)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      IsbnField(draft, enabled, Modifier.weight(1f))
      AsinField(draft, enabled, Modifier.weight(1f))
    }
  }
}

@Composable
private fun FlagFields(
  draft: BookDetailsDraft,
  enabled: Boolean,
  canSeeExplicit: Boolean,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier) {
    ExplicitField(draft, enabled, canSeeExplicit)
    AbridgedField(draft, enabled)
  }
}

@Composable
private fun TitleField(
  draft: BookDetailsDraft,
  enabled: Boolean,
  modifier: Modifier = Modifier,
) {
  TextDetailField(
    state = draft.title,
    label = Res.string.label_title,
    changed = draft.isChanged(BookDetail.Title),
    enabled = enabled,
    error = Res.string.error_title_required.takeIf { draft.isTitleMissing },
    capitalization = KeyboardCapitalization.Sentences,
    modifier = modifier.testTag("field_title"),
  )
}

@Composable
private fun SubtitleField(
  draft: BookDetailsDraft,
  enabled: Boolean,
  modifier: Modifier = Modifier,
) {
  TextDetailField(
    state = draft.subtitle,
    label = Res.string.label_subtitle,
    changed = draft.isChanged(BookDetail.Subtitle),
    enabled = enabled,
    placeholder = Res.string.placeholder_optional,
    capitalization = KeyboardCapitalization.Sentences,
    modifier = modifier.testTag("field_subtitle"),
  )
}

@Composable
private fun AuthorsField(
  draft: BookDetailsDraft,
  suggestions: DetailSuggestions,
  enabled: Boolean,
  modifier: Modifier = Modifier,
) {
  TokenField(
    state = draft.authors,
    label = Res.string.label_authors,
    changed = draft.isChanged(BookDetail.Authors),
    suggestions = suggestions.authors,
    enabled = enabled,
    tag = "field_authors",
    modifier = modifier,
  )
}

@Composable
private fun NarratorsField(
  draft: BookDetailsDraft,
  suggestions: DetailSuggestions,
  enabled: Boolean,
  modifier: Modifier = Modifier,
) {
  TokenField(
    state = draft.narrators,
    label = Res.string.label_narrators,
    changed = draft.isChanged(BookDetail.Narrators),
    suggestions = suggestions.narrators,
    enabled = enabled,
    tag = "field_narrators",
    modifier = modifier,
  )
}

@Composable
private fun PublisherField(
  draft: BookDetailsDraft,
  suggestions: DetailSuggestions,
  enabled: Boolean,
  modifier: Modifier = Modifier,
) {
  SuggestingTextField(
    state = draft.publisher,
    suggestions = suggestions.publishers,
    enabled = enabled,
    modifier = modifier,
  ) { anchor ->
    TextDetailField(
      state = draft.publisher,
      label = Res.string.label_publisher,
      changed = draft.isChanged(BookDetail.Publisher),
      enabled = enabled,
      capitalization = KeyboardCapitalization.Sentences,
      modifier = anchor.fillMaxWidth().testTag("field_publisher"),
    )
  }
}

@Composable
private fun PublishedYearField(
  draft: BookDetailsDraft,
  enabled: Boolean,
  modifier: Modifier = Modifier,
) {
  TextDetailField(
    state = draft.publishedYear,
    label = Res.string.label_published_year,
    changed = draft.isChanged(BookDetail.PublishedYear),
    enabled = enabled,
    error = Res.string.error_year_invalid.takeIf { draft.isPublishedYearInvalid },
    keyboardType = KeyboardType.Number,
    modifier = modifier.testTag("field_published_year"),
  )
}

@Composable
private fun LanguageField(
  draft: BookDetailsDraft,
  suggestions: DetailSuggestions,
  enabled: Boolean,
  modifier: Modifier = Modifier,
) {
  SuggestingTextField(
    state = draft.language,
    suggestions = suggestions.languages,
    enabled = enabled,
    modifier = modifier,
  ) { anchor ->
    TextDetailField(
      state = draft.language,
      label = Res.string.label_language,
      changed = draft.isChanged(BookDetail.Language),
      enabled = enabled,
      capitalization = KeyboardCapitalization.Sentences,
      modifier = anchor.fillMaxWidth().testTag("field_language"),
    )
  }
}

@Composable
private fun GenresField(
  draft: BookDetailsDraft,
  suggestions: DetailSuggestions,
  enabled: Boolean,
  modifier: Modifier = Modifier,
) {
  TokenField(
    state = draft.genres,
    label = Res.string.label_genres,
    changed = draft.isChanged(BookDetail.Genres),
    suggestions = suggestions.genres,
    enabled = enabled,
    tag = "field_genres",
    modifier = modifier,
  )
}

@Composable
private fun TagsField(
  draft: BookDetailsDraft,
  suggestions: DetailSuggestions,
  enabled: Boolean,
  modifier: Modifier = Modifier,
) {
  TokenField(
    state = draft.tags,
    label = Res.string.label_tags,
    changed = draft.isChanged(BookDetail.Tags),
    suggestions = suggestions.tags,
    enabled = enabled,
    tag = "field_tags",
    capitalization = KeyboardCapitalization.Sentences,
    modifier = modifier,
  )
}

@Composable
private fun IsbnField(
  draft: BookDetailsDraft,
  enabled: Boolean,
  modifier: Modifier = Modifier,
) {
  TextDetailField(
    state = draft.isbn,
    label = Res.string.label_isbn,
    changed = draft.isChanged(BookDetail.Isbn),
    enabled = enabled,
    keyboardType = KeyboardType.Ascii,
    modifier = modifier.testTag("field_isbn"),
  )
}

@Composable
private fun AsinField(
  draft: BookDetailsDraft,
  enabled: Boolean,
  modifier: Modifier = Modifier,
) {
  TextDetailField(
    state = draft.asin,
    label = Res.string.label_asin,
    changed = draft.isChanged(BookDetail.Asin),
    enabled = enabled,
    keyboardType = KeyboardType.Ascii,
    capitalization = KeyboardCapitalization.Characters,
    monospace = true,
    modifier = modifier.testTag("field_asin"),
  )
}

@Composable
private fun ExplicitField(
  draft: BookDetailsDraft,
  enabled: Boolean,
  canSeeExplicit: Boolean,
  modifier: Modifier = Modifier,
) {
  SwitchDetailRow(
    label = Res.string.label_explicit,
    checked = draft.explicit,
    onCheckedChange = { draft.explicit = it },
    changed = draft.isChanged(BookDetail.Explicit),
    enabled = enabled,
    warning = Res.string.warning_explicit_hidden.takeIf {
      !canSeeExplicit && draft.explicit && draft.isChanged(BookDetail.Explicit)
    },
    modifier = modifier.testTag("field_explicit"),
  )
}

@Composable
private fun AbridgedField(
  draft: BookDetailsDraft,
  enabled: Boolean,
  modifier: Modifier = Modifier,
) {
  SwitchDetailRow(
    label = Res.string.label_abridged,
    checked = draft.abridged,
    onCheckedChange = { draft.abridged = it },
    changed = draft.isChanged(BookDetail.Abridged),
    enabled = enabled,
    modifier = modifier.testTag("field_abridged"),
  )
}

@Composable
private fun TextDetailField(
  state: TextFieldState,
  label: StringResource,
  changed: Boolean,
  enabled: Boolean,
  modifier: Modifier = Modifier,
  placeholder: StringResource? = null,
  error: StringResource? = null,
  keyboardType: KeyboardType = KeyboardType.Text,
  capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
  monospace: Boolean = false,
) {
  OutlinedTextField(
    state = state,
    enabled = enabled,
    label = { FieldLabel(text = stringResource(label), changed = changed) },
    placeholder = placeholder?.let { { Text(stringResource(it)) } },
    isError = error != null,
    supportingText = error?.let { { Text(stringResource(it)) } },
    lineLimits = TextFieldLineLimits.SingleLine,
    keyboardOptions = KeyboardOptions(
      capitalization = capitalization,
      keyboardType = keyboardType,
    ),
    textStyle = if (monospace) {
      MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace)
    } else {
      MaterialTheme.typography.bodyLarge
    },
    modifier = modifier,
  )
}

@Composable
private fun SwitchDetailRow(
  label: StringResource,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  changed: Boolean,
  enabled: Boolean,
  modifier: Modifier = Modifier,
  warning: StringResource? = null,
) {
  Row(
    horizontalArrangement = Arrangement.spacedBy(16.dp),
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
      .fillMaxWidth()
      .toggleable(
        value = checked,
        enabled = enabled,
        role = Role.Switch,
        onValueChange = onCheckedChange,
      )
      .padding(vertical = 8.dp),
  ) {
    Column(modifier = Modifier.weight(1f)) {
      ProvideTextStyle(MaterialTheme.typography.bodyLarge) {
        FieldLabel(text = stringResource(label), changed = changed)
      }
      if (warning != null) {
        Text(
          text = stringResource(warning),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.error,
        )
      }
    }
    Switch(
      checked = checked,
      onCheckedChange = null,
      enabled = enabled,
    )
  }
}

/** A field's label, marked when the field has changed */
@Composable
internal fun FieldLabel(
  text: String,
  changed: Boolean,
  modifier: Modifier = Modifier,
) {
  Row(
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier,
  ) {
    if (changed) {
      val changedLabel = stringResource(Res.string.cd_changed)
      Box(
        Modifier
          .size(6.dp)
          .background(MaterialTheme.colorScheme.primary, CircleShape)
          .semantics { contentDescription = changedLabel },
      )
    }
    Text(text)
  }
}

@Composable
private fun SectionHeader(
  text: StringResource,
  modifier: Modifier = Modifier,
  changed: Boolean = false,
) {
  ProvideTextStyle(MaterialTheme.typography.titleSmall.copy(color = MaterialTheme.colorScheme.primary)) {
    FieldLabel(
      text = stringResource(text),
      changed = changed,
      modifier = modifier.padding(top = 8.dp),
    )
  }
}

@Composable
private fun Banner(
  text: String,
  modifier: Modifier = Modifier,
  actions: (@Composable () -> Unit)? = null,
) {
  Surface(
    color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    shape = MaterialTheme.shapes.medium,
    modifier = modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 4.dp)) {
      Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(end = 8.dp, bottom = if (actions == null) 8.dp else 0.dp),
      )
      if (actions != null) {
        Row(
          horizontalArrangement = Arrangement.End,
          modifier = Modifier.fillMaxWidth(),
        ) {
          actions()
        }
      }
    }
  }
}

private val MaxFormWidth = 840.dp
private val GroupSpacing = 20.dp
private val FieldSpacing = 12.dp
private val YearFieldWidth = 112.dp
private val SequenceFieldWidth = 96.dp
private val LanguageFieldWidth = 168.dp

// Puts the button level with the field beside it, whose label sits above its outline
private val RemoveButtonTopPadding = 8.dp
