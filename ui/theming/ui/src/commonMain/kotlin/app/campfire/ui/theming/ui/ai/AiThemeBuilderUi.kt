package app.campfire.ui.theming.ui.ai

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.theme.LocalUseDarkColors
import app.campfire.common.compose.widgets.CampfireTopAppBar
import app.campfire.core.di.UserScope
import app.campfire.ui.theming.api.HalogenStyle
import app.campfire.ui.theming.api.colorScheme
import app.campfire.ui.theming.api.screen.AiThemeBuilderScreen
import app.campfire.ui.theming.ui.ai.composables.AppPreview
import app.campfire.ui.theming.ui.ai.emptystate.ShapeParticleEmptyState
import app.campfire.ui.theming.ui.ai.emptystate.ShapeParticleGameState
import app.campfire.ui.theming.ui.builder.composables.IconPicker
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import kotlin.time.Duration.Companion.nanoseconds

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@CircuitInject(AiThemeBuilderScreen::class, UserScope::class)
@Composable
fun AiThemeBuilder(
  state: AiThemeBuilderUiState,
  modifier: Modifier = Modifier,
) {
  val useDarkColor = LocalUseDarkColors.current
  var isDarkMode by remember { mutableStateOf(useDarkColor) }

  // The preview color scheme: generated theme if available, else the host scheme.
  val previewColorScheme = state.theme
    ?.let { colorScheme(it, isDarkMode) }

  val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
  Scaffold(
    topBar = {
      CampfireTopAppBar(
        title = { Text("AI Theme Builder") },
        navigationIcon = {
          IconButton(onClick = { state.eventSink(AiThemeBuilderUiEvent.Back) }) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
          }
        },
        scrollBehavior = scrollBehavior,
        containerColor = Color.Transparent,
        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
      )
    },
    modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
  ) { paddingValues ->
    ShapeParticleEmptyState(
      maxParticles = 130,
      minDistance = 50.dp,
      backfillInterval = 50.nanoseconds,
      state = when {
        state.isGenerating -> ShapeParticleGameState.Emitting
        state.theme != null -> ShapeParticleGameState.End
        else -> ShapeParticleGameState.Idle
      },
      modifier = Modifier.fillMaxSize(),
    )

    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(
          start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
          end = paddingValues.calculateEndPadding(LocalLayoutDirection.current),
        )
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Spacer(Modifier.height(4.dp))

      AnimatedContent(
        targetState = state.theme,
        modifier = Modifier
          .padding(
            top = paddingValues.calculateTopPadding(),
          ),
      ) { theme ->
        if (theme != null) {
          SaveSection(state)
        } else {
          PromptSection(state)
        }
      }

      state.errorMessage?.let { message ->
        ErrorBanner(
          message = message,
          onDismiss = { state.eventSink(AiThemeBuilderUiEvent.DismissError) },
        )
      }

      AnimatedVisibility(
        visible = previewColorScheme != null,
      ) {
        MaterialExpressiveTheme(
          colorScheme = previewColorScheme,
        ) {
          AppPreview(
            isDarkMode = isDarkMode,
            onDarkModeChange = { isDarkMode = it },
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp),
          )
        }
      }

      Spacer(Modifier.height(paddingValues.calculateBottomPadding() + 16.dp))
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PromptSection(
  state: AiThemeBuilderUiState,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp),
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    contentColor = MaterialTheme.colorScheme.onSurface,
    shadowElevation = 1.dp,
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text("Describe your theme", style = MaterialTheme.typography.titleMedium)

      Spacer(Modifier.height(4.dp))

      Text(
        text = "Try \"sunrise over a misty pine forest\" or \"neon-lit cyberpunk arcade.\"",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.height(12.dp))
      OutlinedTextField(
        state = state.prompt,
        label = { Text("Describe a vibe…") },
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(
          capitalization = KeyboardCapitalization.Sentences,
          imeAction = ImeAction.Done,
        ),
        enabled = !state.isGenerating,
      )
      Spacer(Modifier.height(12.dp))
      StyleDropdown(
        selected = state.style,
        onSelect = { state.eventSink(AiThemeBuilderUiEvent.StylePicked(it)) },
        modifier = Modifier.fillMaxWidth(),
      )
      Spacer(Modifier.height(12.dp))

      Button(
        onClick = { state.eventSink(AiThemeBuilderUiEvent.Generate) },
        enabled = !state.isGenerating,
        modifier = Modifier.fillMaxWidth(),
      ) {
        if (state.isGenerating) {
          CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.onPrimary,
          )
          Spacer(Modifier.size(8.dp))
          Text("Generating…")
        } else {
          Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
          Spacer(Modifier.size(8.dp))
          Text("Generate theme")
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SaveSection(
  state: AiThemeBuilderUiState,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp),
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    contentColor = MaterialTheme.colorScheme.onSurface,
    shadowElevation = 1.dp,
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = state.prompt.text.toString(),
        style = MaterialTheme.typography.titleMedium,
      )

      Spacer(Modifier.height(16.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        IconPicker(
          icon = state.icon,
          onIconClick = { state.eventSink(AiThemeBuilderUiEvent.IconPicked(it)) },
        )
        OutlinedTextField(
          state = state.name,
          label = { Text("Theme name") },
          shape = MaterialTheme.shapes.medium,
          modifier = Modifier.weight(1f),
        )
      }

      Spacer(Modifier.height(16.dp))

      val saveButtonSize = ButtonDefaults.MinHeight
      Button(
        enabled = state.isSavable,
        onClick = { state.eventSink(AiThemeBuilderUiEvent.Save) },
        shapes = ButtonDefaults.shapesFor(saveButtonSize),
        contentPadding = ButtonDefaults.contentPaddingFor(saveButtonSize),
        modifier = Modifier
          .heightIn(saveButtonSize)
          .fillMaxWidth(),
      ) {
        Icon(
          Icons.Rounded.Save,
          contentDescription = "Save new theme",
          modifier = Modifier
            .size(ButtonDefaults.iconSizeFor(saveButtonSize)),
        )
        Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(saveButtonSize)))
        Text(
          text = "Save theme",
          style = ButtonDefaults.textStyleFor(saveButtonSize),
        )
      }

      Button(
        onClick = { state.eventSink(AiThemeBuilderUiEvent.Clear) },
        shapes = ButtonDefaults.shapesFor(saveButtonSize),
        contentPadding = ButtonDefaults.contentPaddingFor(saveButtonSize),
        colors = ButtonDefaults.filledTonalButtonColors(),
        modifier = Modifier
          .heightIn(saveButtonSize)
          .fillMaxWidth(),
      ) {
        Icon(
          Icons.Rounded.AutoAwesome,
          contentDescription = "Generate again",
          modifier = Modifier
            .size(ButtonDefaults.iconSizeFor(saveButtonSize)),
        )
        Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(saveButtonSize)))
        Text(
          text = "Generate something else",
          style = ButtonDefaults.textStyleFor(saveButtonSize),
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StyleDropdown(
  selected: HalogenStyle,
  onSelect: (HalogenStyle) -> Unit,
  modifier: Modifier = Modifier,
) {
  var expanded by remember { mutableStateOf(false) }

  Box(modifier = modifier) {
    Surface(
      onClick = { expanded = true },
      shape = MaterialTheme.shapes.medium,
      color = MaterialTheme.colorScheme.surfaceContainerHighest,
      contentColor = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Style",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = selected.displayName,
            style = MaterialTheme.typography.bodyLarge,
          )
        }
        Icon(
          Icons.Rounded.ArrowDropDown,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    DropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
    ) {
      HalogenStyle.entries.forEach { style ->
        DropdownMenuItem(
          text = { Text(style.displayName) },
          onClick = {
            onSelect(style)
            expanded = false
          },
        )
      }
    }
  }
}

@Composable
private fun ErrorBanner(
  message: String,
  onDismiss: () -> Unit,
) {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp),
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.errorContainer,
    contentColor = MaterialTheme.colorScheme.onErrorContainer,
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.weight(1f),
      )
      TextButton(onClick = onDismiss) {
        Text("Dismiss", color = MaterialTheme.colorScheme.onErrorContainer)
      }
    }
  }
}
