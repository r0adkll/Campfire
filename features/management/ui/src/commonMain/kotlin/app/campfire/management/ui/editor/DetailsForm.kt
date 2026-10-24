// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.management.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import campfire.features.management.ui.generated.resources.Res
import campfire.features.management.ui.generated.resources.action_keep_mine
import campfire.features.management.ui.generated.resources.action_reload
import campfire.features.management.ui.generated.resources.banner_behind_server
import campfire.features.management.ui.generated.resources.banner_cant_edit
import campfire.features.management.ui.generated.resources.cd_changed
import campfire.features.management.ui.generated.resources.editor_footer
import campfire.features.management.ui.generated.resources.error_title_required
import campfire.features.management.ui.generated.resources.error_year_invalid
import campfire.features.management.ui.generated.resources.label_abridged
import campfire.features.management.ui.generated.resources.label_asin
import campfire.features.management.ui.generated.resources.label_description
import campfire.features.management.ui.generated.resources.label_explicit
import campfire.features.management.ui.generated.resources.label_isbn
import campfire.features.management.ui.generated.resources.label_language
import campfire.features.management.ui.generated.resources.label_published_year
import campfire.features.management.ui.generated.resources.label_publisher
import campfire.features.management.ui.generated.resources.label_subtitle
import campfire.features.management.ui.generated.resources.label_title
import campfire.features.management.ui.generated.resources.placeholder_optional
import campfire.features.management.ui.generated.resources.section_identifiers
import campfire.features.management.ui.generated.resources.section_publishing
import campfire.features.management.ui.generated.resources.warning_explicit_hidden
import com.mohamedrejeb.richeditor.ui.material3.OutlinedRichTextEditor
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The book's details as fields, in one column or, with [twoColumns], two: what it is on the
 * leading side, how it was published and identified on the trailing one.
 */
@Composable
internal fun DetailsForm(
  draft: BookDetailsDraft,
  enabled: Boolean,
  canEdit: Boolean,
  canSeeExplicit: Boolean,
  isBehindServer: Boolean,
  onReload: () -> Unit,
  onKeepMine: () -> Unit,
  twoColumns: Boolean,
  modifier: Modifier = Modifier,
) {
  Box(
    contentAlignment = Alignment.TopCenter,
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .imePadding(),
  ) {
    Column(
      verticalArrangement = Arrangement.spacedBy(GroupSpacing),
      modifier = Modifier
        .widthIn(max = MaxFormWidth)
        .fillMaxWidth()
        .padding(16.dp),
    ) {
      if (!canEdit) {
        Banner(text = stringResource(Res.string.banner_cant_edit))
      }
      if (isBehindServer) {
        Banner(text = stringResource(Res.string.banner_behind_server)) {
          TextButton(onClick = onKeepMine, modifier = Modifier.testTag("editor_keep_mine")) {
            Text(stringResource(Res.string.action_keep_mine))
          }
          TextButton(onClick = onReload, modifier = Modifier.testTag("editor_reload")) {
            Text(stringResource(Res.string.action_reload))
          }
        }
      }

      if (twoColumns) {
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
          Column(
            verticalArrangement = Arrangement.spacedBy(GroupSpacing),
            modifier = Modifier.weight(1f),
          ) {
            IdentityFields(draft, enabled)
            DescriptionField(draft, enabled)
          }
          Column(
            verticalArrangement = Arrangement.spacedBy(GroupSpacing),
            modifier = Modifier.weight(1f),
          ) {
            PublishingFields(draft, enabled)
            IdentifierFields(draft, enabled)
            FlagFields(draft, enabled, canSeeExplicit)
          }
        }
      } else {
        IdentityFields(draft, enabled)
        DescriptionField(draft, enabled)
        PublishingFields(draft, enabled)
        IdentifierFields(draft, enabled)
        FlagFields(draft, enabled, canSeeExplicit)
      }

      Text(
        text = stringResource(Res.string.editor_footer),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
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
    TextDetailField(
      state = draft.title,
      label = Res.string.label_title,
      changed = draft.isChanged(BookDetail.Title),
      enabled = enabled,
      error = Res.string.error_title_required.takeIf { draft.isTitleMissing },
      capitalization = KeyboardCapitalization.Words,
      modifier = Modifier.fillMaxWidth().testTag("field_title"),
    )
    TextDetailField(
      state = draft.subtitle,
      label = Res.string.label_subtitle,
      changed = draft.isChanged(BookDetail.Subtitle),
      enabled = enabled,
      placeholder = Res.string.placeholder_optional,
      capitalization = KeyboardCapitalization.Words,
      modifier = Modifier.fillMaxWidth().testTag("field_subtitle"),
    )
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
  enabled: Boolean,
  modifier: Modifier = Modifier,
) {
  Column(
    verticalArrangement = Arrangement.spacedBy(FieldSpacing),
    modifier = modifier,
  ) {
    SectionHeader(Res.string.section_publishing)
    TextDetailField(
      state = draft.publisher,
      label = Res.string.label_publisher,
      changed = draft.isChanged(BookDetail.Publisher),
      enabled = enabled,
      capitalization = KeyboardCapitalization.Words,
      modifier = Modifier.fillMaxWidth().testTag("field_publisher"),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      TextDetailField(
        state = draft.publishedYear,
        label = Res.string.label_published_year,
        changed = draft.isChanged(BookDetail.PublishedYear),
        enabled = enabled,
        error = Res.string.error_year_invalid.takeIf { draft.isPublishedYearInvalid },
        keyboardType = KeyboardType.Number,
        modifier = Modifier.width(YearFieldWidth).testTag("field_published_year"),
      )
      TextDetailField(
        state = draft.language,
        label = Res.string.label_language,
        changed = draft.isChanged(BookDetail.Language),
        enabled = enabled,
        capitalization = KeyboardCapitalization.Words,
        modifier = Modifier.weight(1f).testTag("field_language"),
      )
    }
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
      TextDetailField(
        state = draft.isbn,
        label = Res.string.label_isbn,
        changed = draft.isChanged(BookDetail.Isbn),
        enabled = enabled,
        keyboardType = KeyboardType.Ascii,
        modifier = Modifier.weight(1f).testTag("field_isbn"),
      )
      TextDetailField(
        state = draft.asin,
        label = Res.string.label_asin,
        changed = draft.isChanged(BookDetail.Asin),
        enabled = enabled,
        keyboardType = KeyboardType.Ascii,
        capitalization = KeyboardCapitalization.Characters,
        monospace = true,
        modifier = Modifier.weight(1f).testTag("field_asin"),
      )
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
    SwitchDetailRow(
      label = Res.string.label_explicit,
      checked = draft.explicit,
      onCheckedChange = { draft.explicit = it },
      changed = draft.isChanged(BookDetail.Explicit),
      enabled = enabled,
      warning = Res.string.warning_explicit_hidden.takeIf {
        !canSeeExplicit && draft.explicit && draft.isChanged(BookDetail.Explicit)
      },
      modifier = Modifier.testTag("field_explicit"),
    )
    SwitchDetailRow(
      label = Res.string.label_abridged,
      checked = draft.abridged,
      onCheckedChange = { draft.abridged = it },
      changed = draft.isChanged(BookDetail.Abridged),
      enabled = enabled,
      modifier = Modifier.testTag("field_abridged"),
    )
  }
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
private fun FieldLabel(
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
) {
  Text(
    text = stringResource(text),
    style = MaterialTheme.typography.titleSmall,
    color = MaterialTheme.colorScheme.primary,
    modifier = modifier.padding(top = 8.dp),
  )
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
