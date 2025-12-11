package app.campfire.ui.theming.ui.builder

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallExtendedFloatingActionButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.CampfireWindowInsets
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.layout.ContentLayout
import app.campfire.common.compose.layout.LocalContentLayout
import app.campfire.common.compose.theme.LocalUseDarkColors
import app.campfire.common.compose.theme.alt.AltRedColorPalette
import app.campfire.common.compose.widgets.CampfireTopAppBar
import app.campfire.core.di.UserScope
import app.campfire.ui.theming.api.AppTheme
import app.campfire.ui.theming.api.colorScheme
import app.campfire.ui.theming.api.screen.ThemeBuilderScreen
import app.campfire.ui.theming.ui.builder.composables.ColorPicker
import app.campfire.ui.theming.ui.builder.composables.ColorSpecPicker
import app.campfire.ui.theming.ui.builder.composables.ColorStylePicker
import app.campfire.ui.theming.ui.builder.composables.ContrastPicker
import app.campfire.ui.theming.ui.builder.composables.Header
import app.campfire.ui.theming.ui.builder.composables.IconPicker
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.r0adkll.swatchbuckler.color.dynamiccolor.ColorSpec.SpecVersion
import com.r0adkll.swatchbuckler.color.dynamiccolor.Variant
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@CircuitInject(ThemeBuilderScreen::class, UserScope::class)
@Composable
fun ThemeBuilder(
  state: ThemeBuilderUiState,
  modifier: Modifier = Modifier,
) {
  val useDarkColor = LocalUseDarkColors.current
  var isDarkMode by remember { mutableStateOf(useDarkColor) }
  MaterialExpressiveTheme(
    colorScheme = colorScheme(state.theme, isDarkMode),
  ) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
      topBar = {
        CampfireTopAppBar(
          title = {
            Text("Theme Builder")
          },
          navigationIcon = {
            IconButton(
              onClick = { state.eventSink(ThemeBuilderUiEvent.Back) },
            ) {
              Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
          },
          actions = {
            var showDeleteConfirmation by remember { mutableStateOf(false) }
            if (!state.theme.isNew) {
              IconButton(
                onClick = {
                  showDeleteConfirmation = true
                },
              ) {
                Icon(
                  Icons.Rounded.Delete,
                  contentDescription = "Delete",
                  tint = MaterialTheme.colorScheme.error,
                )
              }

              if (showDeleteConfirmation) {
                AlertDialog(
                  onDismissRequest = { showDeleteConfirmation = false },
                  title = { Text("Delete theme?") },
                  text = { Text("Are you sure you want to delete this theme?") },
                  confirmButton = {
                    TextButton(
                      onClick = {
                        state.eventSink(ThemeBuilderUiEvent.Delete)
                        showDeleteConfirmation = false
                      },
                    ) {
                      Text("Delete")
                    }
                  },
                  dismissButton = {
                    TextButton(
                      onClick = { showDeleteConfirmation = false },
                    ) {
                      Text("Cancel")
                    }
                  },
                )
              }
            }

            Switch(
              checked = isDarkMode,
              onCheckedChange = { isDarkMode = it },
              thumbContent = {
                Icon(
                  if (!isDarkMode) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                  contentDescription = null,
                  modifier = Modifier.size(SwitchDefaults.IconSize),
                  tint = if (!isDarkMode) {
                    LocalContentColor.current
                  } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                  },
                )
              },
              modifier = Modifier.padding(horizontal = 8.dp),
            )
          },
          scrollBehavior = scrollBehavior,
          containerColor = MaterialTheme.colorScheme.surfaceContainer,
          scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        )
      },
      floatingActionButton = {
        val expanded by remember {
          derivedStateOf {
            scrollBehavior.state.overlappedFraction < 0.5f
          }
        }

        AnimatedVisibility(
          visible = state.isCreatable,
        ) {
          SmallExtendedFloatingActionButton(
            expanded = expanded,
            text = { Text("Save") },
            icon = { Icon(Icons.Rounded.Save, contentDescription = "Save theme") },
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            onClick = {
              state.eventSink(ThemeBuilderUiEvent.Save)
            },
          )
        }
      },
      containerColor = MaterialTheme.colorScheme.surfaceContainer,
      modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
      contentWindowInsets = CampfireWindowInsets,
    ) { paddingValues ->
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(
            top = paddingValues.calculateTopPadding(),
            start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
            end = paddingValues.calculateEndPadding(LocalLayoutDirection.current),
          )
          .verticalScroll(rememberScrollState()),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(
              horizontal = 16.dp,
              vertical = 8.dp,
            ),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          IconPicker(
            icon = state.theme.icon,
            onIconClick = {
              state.eventSink(ThemeBuilderUiEvent.IconPicked(it))
            },
          )

          OutlinedTextField(
            state = state.name,
            label = { Text("Name") },
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.weight(1f),
          )
        }

        ColorPicker(
          title = "Seed color",
          description = "Used to generate the primary, secondary, and tertiary palettes of your new theme",
          color = state.seedColor,
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          onColorChange = {
            state.eventSink(ThemeBuilderUiEvent.SeedColorPicked(it))
          },
          modifier = Modifier.padding(
            horizontal = 16.dp,
            vertical = 8.dp,
          ),
        )

        Spacer(Modifier.height(8.dp))

        Header(
          text = "Color Spec",
          description = "Choose the spec used for generating the dynamic color palette. The 2025 spec only " +
            "supports Tonal, Neutral, Vibrant, and Expressive.",
          modifier = Modifier.padding(vertical = 8.dp),
        )

        ColorSpecPicker(
          spec = state.colorSpec,
          onSpecClick = {
            state.eventSink(ThemeBuilderUiEvent.ColorSpecClick(it))
          },
          modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(Modifier.height(16.dp))

        Header(
          text = "Style",
          description = "Pick the dynamic color theme to generate your new palette with.",
          modifier = Modifier.padding(vertical = 8.dp),
        )

        ColorStylePicker(
          spec = state.colorSpec,
          style = state.colorStyle,
          onStyleClick = {
            state.eventSink(ThemeBuilderUiEvent.ColorStyleClick(it))
          },
          modifier = Modifier
            .padding(horizontal = 16.dp),
        )

        Header(
          text = "Contrast",
          modifier = Modifier.padding(vertical = 8.dp),
        )

        ContrastPicker(
          level = state.contrastLevel,
          onLevelClick = {
            state.eventSink(ThemeBuilderUiEvent.ContrastLevelClick(it))
          },
          modifier = Modifier.padding(horizontal = 16.dp),
        )

        Header(
          text = "Colors",
          modifier = Modifier.padding(vertical = 8.dp),
        )

        ColorPicker(
          color = MaterialTheme.colorScheme.primary,
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          title = "Primary",
          description = "Setting this will reset all other overrides. This is the same as setting the seed color.",
          onColorChange = { color ->
            state.eventSink(ThemeBuilderUiEvent.SeedColorPicked(color))
          },
          modifier = Modifier.padding(horizontal = 16.dp),
          colorSize = 40.dp,
          contentPadding = PaddingValues(
            horizontal = 16.dp,
            vertical = 12.dp,
          ),
        )

        Spacer(Modifier.height(4.dp))

        ColorPicker(
          color = MaterialTheme.colorScheme.secondary,
          containerColor = MaterialTheme.colorScheme.secondaryContainer,
          title = "Secondary",
          description = state.secondaryColorOverride?.let { "Overridden" },
          onColorChange = { color ->
            state.eventSink(ThemeBuilderUiEvent.SecondaryColorPicked(color))
          },
          modifier = Modifier.padding(horizontal = 16.dp),
          colorSize = 40.dp,
          contentPadding = PaddingValues(
            horizontal = 16.dp,
            vertical = 12.dp,
          ),
        )

        Spacer(Modifier.height(4.dp))

        ColorPicker(
          color = MaterialTheme.colorScheme.tertiary,
          containerColor = MaterialTheme.colorScheme.tertiaryContainer,
          title = "Tertiary",
          description = state.tertiaryColorOverride?.let { "Overridden" },
          onColorChange = { color ->
            state.eventSink(ThemeBuilderUiEvent.TertiaryColorPicked(color))
          },
          modifier = Modifier.padding(horizontal = 16.dp),
          colorSize = 40.dp,
          contentPadding = PaddingValues(
            horizontal = 16.dp,
            vertical = 12.dp,
          ),
        )

        Spacer(Modifier.height(4.dp))

        ColorPicker(
          color = MaterialTheme.colorScheme.error,
          containerColor = MaterialTheme.colorScheme.errorContainer,
          title = "Error",
          description = state.errorColorOverride?.let { "Overridden" },
          onColorChange = { color ->
            state.eventSink(ThemeBuilderUiEvent.ErrorColorPicked(color))
          },
          modifier = Modifier.padding(horizontal = 16.dp),
          colorSize = 40.dp,
          contentPadding = PaddingValues(
            horizontal = 16.dp,
            vertical = 12.dp,
          ),
        )

        Spacer(Modifier.height(4.dp))

        ColorPicker(
          color = MaterialTheme.colorScheme.surface,
          containerColor = MaterialTheme.colorScheme.surfaceContainer,
          title = "Neutral",
          description = state.neutralColorOverride?.let { "Overridden" },
          onColorChange = { color ->
            state.eventSink(ThemeBuilderUiEvent.NeutralColorPicked(color))
          },
          modifier = Modifier.padding(horizontal = 16.dp),
          colorSize = 40.dp,
          contentPadding = PaddingValues(
            horizontal = 16.dp,
            vertical = 12.dp,
          ),
        )

        Spacer(Modifier.height(4.dp))

        ColorPicker(
          color = MaterialTheme.colorScheme.surfaceVariant,
          containerColor = MaterialTheme.colorScheme.surfaceContainer,
          title = "Neutral Variant",
          description = state.neutralVariantColorOverride?.let { "Overridden" },
          onColorChange = { color ->
            state.eventSink(ThemeBuilderUiEvent.NeutralVariantColorPicked(color))
          },
          modifier = Modifier.padding(horizontal = 16.dp),
          colorSize = 40.dp,
          contentPadding = PaddingValues(
            horizontal = 16.dp,
            vertical = 12.dp,
          ),
        )

        Spacer(
          Modifier.padding(
            bottom = paddingValues.calculateBottomPadding(),
          ),
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Preview
@Composable
fun ThemeBuilderPreview() {
  CompositionLocalProvider(
    LocalWindowSizeClass provides calculateWindowSizeClass(),
    LocalContentLayout provides ContentLayout.Root,
  ) {
    var seedColor by remember { mutableStateOf(Color.Red) }
    var colorSpec by remember { mutableStateOf(SpecVersion.SPEC_2021) }
    var colorStyle by remember { mutableStateOf(Variant.EXPRESSIVE) }
    var contrastLevel by remember { mutableStateOf(ContrastLevel.Normal) }

    ThemeBuilder(
      state = ThemeBuilderUiState(
        theme = AppTheme.Fixed.Custom(
          id = "test",
          name = "Test",
          icon = AppTheme.Icon.Tent,
          seedColor = Color.Red,
          colorSpec = SpecVersion.SPEC_2021,
          colorStyle = Variant.EXPRESSIVE,
          contrastLevel = 0f,
          secondaryColorOverride = null,
          tertiaryColorOverride = null,
          errorColorOverride = null,
          neutralColorOverride = null,
          neutralVariantColorOverride = null,
          colorPalette = AltRedColorPalette,
        ),
        name = rememberTextFieldState(""),
        seedColor = seedColor,
        secondaryColorOverride = null,
        tertiaryColorOverride = null,
        errorColorOverride = null,
        neutralColorOverride = null,
        neutralVariantColorOverride = null,
        colorSpec = colorSpec,
        colorStyle = colorStyle,
        contrastLevel = contrastLevel,
        eventSink = { event ->
          when (event) {
            is ThemeBuilderUiEvent.ColorSpecClick -> colorSpec = event.spec
            is ThemeBuilderUiEvent.ColorStyleClick -> colorStyle = event.style
            is ThemeBuilderUiEvent.ContrastLevelClick -> contrastLevel = event.level
            else -> Unit
          }
        },
      ),
    )
  }
}
