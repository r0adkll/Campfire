// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.OutlinedButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import app.campfire.bookinfo.api.ProviderCapabilities
import app.campfire.bookinfo.api.ProviderLinkState
import app.campfire.common.compose.CampfireWindowInsets
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.providerBrandColor
import app.campfire.common.compose.icons.providerBrandIcon
import app.campfire.common.compose.icons.providerBrandSecondaryColor
import app.campfire.common.compose.icons.providerOnBrandColor
import app.campfire.common.compose.icons.rounded.Disconnected
import app.campfire.common.compose.theme.LocalUseDarkColors
import app.campfire.common.compose.widgets.CampfireTopAppBar
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.common.screens.ConnectedProvidersScreen
import app.campfire.core.di.UserScope
import campfire.data.bookinfo.ui.generated.resources.Res
import campfire.data.bookinfo.ui.generated.resources.action_back
import campfire.data.bookinfo.ui.generated.resources.capability_metadata
import campfire.data.bookinfo.ui.generated.resources.capability_ratings
import campfire.data.bookinfo.ui.generated.resources.capability_reviews
import campfire.data.bookinfo.ui.generated.resources.capability_series
import campfire.data.bookinfo.ui.generated.resources.connected_providers_description
import campfire.data.bookinfo.ui.generated.resources.connected_providers_title
import campfire.data.bookinfo.ui.generated.resources.provider_clear_cache
import campfire.data.bookinfo.ui.generated.resources.provider_clear_cache_subtitle
import campfire.data.bookinfo.ui.generated.resources.provider_connect
import campfire.data.bookinfo.ui.generated.resources.provider_disconnect
import campfire.data.bookinfo.ui.generated.resources.provider_get_token
import campfire.data.bookinfo.ui.generated.resources.provider_link_error
import campfire.data.bookinfo.ui.generated.resources.provider_link_invalid
import campfire.data.bookinfo.ui.generated.resources.provider_linked
import campfire.data.bookinfo.ui.generated.resources.provider_linked_as
import campfire.data.bookinfo.ui.generated.resources.provider_token_hint
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import org.jetbrains.compose.resources.stringResource

@CircuitInject(ConnectedProvidersScreen::class, UserScope::class)
@Composable
fun ConnectedProvidersUi(
  state: ConnectedProvidersUiState,
  modifier: Modifier = Modifier,
) {
  val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
  Scaffold(
    topBar = {
      CampfireTopAppBar(
        title = { Text(stringResource(Res.string.connected_providers_title)) },
        navigationIcon = {
          val backLabel = stringResource(Res.string.action_back)
          IconButtonTooltip(text = backLabel) {
            IconButton(
              onClick = { state.eventSink(ConnectedProvidersUiEvent.Back) },
            ) {
              Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = backLabel)
            }
          }
        },
        scrollBehavior = scrollBehavior,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
      )
    },
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
    modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    contentWindowInsets = CampfireWindowInsets,
  ) { paddingValues ->
    LazyColumn(
      contentPadding = paddingValues,
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item("description") {
        Text(
          text = stringResource(Res.string.connected_providers_description),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
      }
      items(state.providers, key = { it.id.key }) { provider ->
        ProviderCard(
          provider = provider,
          eventSink = state.eventSink,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        )
      }
      item("clear_cache") {
        ClearCacheRow(
          isClearing = state.isClearingCache,
          onClick = { state.eventSink(ConnectedProvidersUiEvent.ClearCache) },
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        )
      }
    }
  }
}

@Composable
private fun ClearCacheRow(
  isClearing: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  ListItem(
    headlineContent = {
      Text(stringResource(Res.string.provider_clear_cache))
    },
    supportingContent = {
      Text(stringResource(Res.string.provider_clear_cache_subtitle))
    },
    trailingContent = if (isClearing) {
      {
        CircularProgressIndicator(modifier = Modifier.size(16.dp))
      }
    } else {
      null
    },
    colors = ListItemDefaults.colors(
      containerColor = Color.Transparent,
    ),
    modifier = modifier.clickable(onClick = onClick),
  )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ProviderCard(
  provider: ProviderRowState,
  eventSink: (ConnectedProvidersUiEvent) -> Unit,
  modifier: Modifier = Modifier,
) {
  val surfaceCompositeAlpha = if (LocalUseDarkColors.current) 0.25f else 0.68f
  val providerBrandColor = providerBrandColor(provider.id.key)
  val providerOnBrandColor = providerOnBrandColor(provider.id.key)
  val providerOnBrandVariantColor = providerOnBrandColor?.copy(alpha = 0.68f)
  val providerBrandSecondaryColor = providerBrandSecondaryColor(provider.id.key)
  Surface(
    shape = MaterialTheme.shapes.largeIncreased,
    color = providerBrandColor
      ?.copy(alpha = surfaceCompositeAlpha)
      ?.compositeOver(MaterialTheme.colorScheme.surface)
      ?: MaterialTheme.colorScheme.surface,
    contentColor = providerBrandColor ?: MaterialTheme.colorScheme.onSurface,
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
      ) {
        providerBrandIcon(provider.id.key)?.let { brandIcon ->
          Image(
            imageVector = brandIcon,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
          )
          Spacer(Modifier.width(12.dp))
        }
        Column(
          modifier = Modifier.weight(1f),
        ) {
          Text(
            text = provider.name,
            style = MaterialTheme.typography.titleMedium,
            color = providerOnBrandColor ?: MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = provider.capabilities.summary(),
            style = MaterialTheme.typography.bodySmall,
            color = providerOnBrandVariantColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Spacer(Modifier.width(16.dp))
        Switch(
          checked = provider.enabled,
          onCheckedChange = { checked ->
            eventSink(ConnectedProvidersUiEvent.ToggleEnabled(provider.id, checked))
          },
          colors = if (
            providerBrandColor != null &&
            providerBrandSecondaryColor != null &&
            providerOnBrandColor != null
          ) {
            SwitchDefaults.colors(
              checkedTrackColor = providerBrandColor,
              checkedThumbColor = providerBrandSecondaryColor,
              checkedIconColor = providerOnBrandColor,
            )
          } else SwitchDefaults.colors(),
        )
      }

      if (provider.supportsLinking) {
        Spacer(Modifier.height(12.dp))
        when (val linkState = provider.linkState) {
          is ProviderLinkState.Linked -> LinkedContent(provider, linkState, eventSink)
          is ProviderLinkState.Invalid -> UnlinkedContent(provider, eventSink, invalid = true)
          ProviderLinkState.NotLinked -> UnlinkedContent(provider, eventSink, invalid = false)
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LinkedContent(
  provider: ProviderRowState,
  linkState: ProviderLinkState.Linked,
  eventSink: (ConnectedProvidersUiEvent) -> Unit,
  modifier: Modifier = Modifier,
) {
  val providerOnBrandColor = providerOnBrandColor(provider.id.key)
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier.fillMaxWidth(),
  ) {
    Text(
      text = linkState.accountName
        ?.let { stringResource(Res.string.provider_linked_as, it) }
        ?: stringResource(Res.string.provider_linked),
      style = MaterialTheme.typography.bodyLarge,
      color = providerOnBrandColor
        ?: MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.weight(1f),
    )

    val buttonSize = ButtonDefaults.ExtraSmallContainerHeight
    Button(
      onClick = { eventSink(ConnectedProvidersUiEvent.Unlink(provider.id)) },
      shapes = ButtonDefaults.shapes(),
      colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
      ),
      contentPadding = ButtonDefaults.contentPaddingFor(buttonSize, hasStartIcon = true),
      modifier = Modifier
        .heightIn(buttonSize),
    ) {
      Icon(
        CampfireIcons.Rounded.Disconnected,
        contentDescription = null,
        modifier = Modifier.size(ButtonDefaults.iconSizeFor(buttonSize)),
      )
      Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(buttonSize)))
      Text(
        text = stringResource(Res.string.provider_disconnect),
        style = ButtonDefaults.textStyleFor(buttonSize),
      )
    }
  }
}

@Composable
private fun UnlinkedContent(
  provider: ProviderRowState,
  eventSink: (ConnectedProvidersUiEvent) -> Unit,
  invalid: Boolean,
  modifier: Modifier = Modifier,
) {
  var token by rememberSaveable(provider.id) { mutableStateOf("") }

  val providerBrandColor = providerBrandColor(provider.id.key)
  val providerOnBrandColor = providerOnBrandColor(provider.id.key)
  val providerOnBrandVariantColor = providerOnBrandColor?.copy(alpha = 0.68f)

  Column(modifier = modifier) {
    if (invalid) {
      Text(
        text = stringResource(Res.string.provider_link_invalid),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
      )
      Spacer(Modifier.height(8.dp))
    }
    OutlinedTextField(
      value = token,
      onValueChange = { token = it },
      placeholder = { Text(stringResource(Res.string.provider_token_hint)) },
      singleLine = true,
      isError = provider.linkFailed,
      supportingText = if (provider.linkFailed) {
        { Text(stringResource(Res.string.provider_link_error)) }
      } else {
        null
      },
      modifier = Modifier.fillMaxWidth(),
      colors = if (
        providerBrandColor != null &&
        providerOnBrandColor != null &&
        providerOnBrandVariantColor != null
      ) {
        OutlinedTextFieldDefaults.colors(
          focusedBorderColor = providerBrandColor,
          cursorColor = providerBrandColor,
          unfocusedBorderColor = providerOnBrandVariantColor,
          unfocusedPlaceholderColor = providerOnBrandVariantColor,
          focusedPlaceholderColor = providerOnBrandVariantColor,
          focusedTextColor = providerOnBrandColor,
        )
      } else OutlinedTextFieldDefaults.colors(),
    )
    Spacer(Modifier.height(8.dp))
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth(),
    ) {
      provider.linkHelpUrl?.let { url ->
        OutlinedButton(
          onClick = { eventSink(ConnectedProvidersUiEvent.OpenLinkHelp(url)) },
          colors = ButtonDefaults.outlinedButtonColors(
            contentColor = providerOnBrandColor ?: Color.Unspecified,
          ),
          border = BorderStroke(1.dp, providerOnBrandColor ?: MaterialTheme.colorScheme.outlineVariant),
        ) {
          Text(stringResource(Res.string.provider_get_token))
        }
      }
      Spacer(Modifier.weight(1f))
      if (provider.isVerifying) {
        CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
      } else {
        Button(
          onClick = { eventSink(ConnectedProvidersUiEvent.Link(provider.id, token)) },
          enabled = token.isNotBlank(),
          colors = if (providerBrandColor != null) {
            ButtonDefaults.buttonColors(
              containerColor = providerBrandColor,
              contentColor = providerOnBrandColor ?: MaterialTheme.colorScheme.onPrimary,
            )
          } else {
            ButtonDefaults.buttonColors()
          },
        ) {
          Text(stringResource(Res.string.provider_connect))
        }
      }
    }
  }
}

@Composable
private fun ProviderCapabilities.summary(): String {
  val labels = buildList {
    if (hasAggregateRating) add(stringResource(Res.string.capability_ratings))
    if (hasReviewText) add(stringResource(Res.string.capability_reviews))
    if (hasSeriesOrdering) add(stringResource(Res.string.capability_series))
    if (hasSupplementalMetadata) add(stringResource(Res.string.capability_metadata))
  }
  return labels.joinToString(" · ")
}
