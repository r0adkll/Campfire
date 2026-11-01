// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.builder

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.CampfireWindowInsets
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.back.OverlayPriorityBackHandler
import app.campfire.common.compose.currentWindowSizeClass
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Delete
import app.campfire.common.compose.icons.rounded.MoreVert
import app.campfire.common.compose.layout.ContentLayout
import app.campfire.common.compose.layout.LocalContentLayout
import app.campfire.common.compose.theme.LocalUseDarkColors
import app.campfire.common.compose.theme.colorScheme as paletteColorScheme
import app.campfire.common.compose.widgets.CampfireTopAppBar
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.common.compose.widgets.NavigationBackButton
import app.campfire.core.di.UserScope
import app.campfire.ui.theming.api.AppTheme
import app.campfire.ui.theming.api.colorScheme
import app.campfire.ui.theming.api.screen.ThemeBuilderScreen
import app.campfire.ui.theming.ui.builder.color.ContrastCheck
import app.campfire.ui.theming.ui.builder.color.CustomThemeColors
import app.campfire.ui.theming.ui.builder.color.scheme
import app.campfire.ui.theming.ui.builder.composables.AllColorsPanel
import app.campfire.ui.theming.ui.builder.composables.ColorPickerPanel
import app.campfire.ui.theming.ui.builder.composables.ContrastPicker
import app.campfire.ui.theming.ui.builder.composables.FineTuneParts
import app.campfire.ui.theming.ui.builder.composables.PaletteRows
import app.campfire.ui.theming.ui.builder.composables.ReadabilitySummary
import app.campfire.ui.theming.ui.builder.composables.SaveThemeSheet
import app.campfire.ui.theming.ui.builder.composables.SeedColorSection
import app.campfire.ui.theming.ui.builder.composables.StylePicker
import app.campfire.ui.theming.ui.builder.composables.TunePanel
import app.campfire.ui.theming.ui.preview.DarkModeSwitch
import app.campfire.ui.theming.ui.preview.PreviewContent
import app.campfire.ui.theming.ui.preview.PreviewScene
import app.campfire.ui.theming.ui.preview.PreviewSceneTabs
import app.campfire.ui.theming.ui.preview.ThemePreviewContent
import app.campfire.ui.theming.ui.preview.ThemeTargets
import campfire.ui.theming.ui.generated.resources.Res
import campfire.ui.theming.ui.generated.resources.action_delete_theme
import campfire.ui.theming.ui.generated.resources.action_more_options
import campfire.ui.theming.ui.generated.resources.theme_builder_cancel
import campfire.ui.theming.ui.generated.resources.theme_builder_delete_body
import campfire.ui.theming.ui.generated.resources.theme_builder_delete_confirm
import campfire.ui.theming.ui.generated.resources.theme_builder_delete_title
import campfire.ui.theming.ui.generated.resources.theme_builder_preview_dark
import campfire.ui.theming.ui.generated.resources.theme_builder_preview_light
import campfire.ui.theming.ui.generated.resources.theme_builder_save
import campfire.ui.theming.ui.generated.resources.theme_builder_section_color
import campfire.ui.theming.ui.generated.resources.theme_builder_section_contrast
import campfire.ui.theming.ui.generated.resources.theme_builder_section_fine_tune
import campfire.ui.theming.ui.generated.resources.theme_builder_section_palettes
import campfire.ui.theming.ui.generated.resources.theme_builder_section_readability
import campfire.ui.theming.ui.generated.resources.theme_builder_section_style
import campfire.ui.theming.ui.generated.resources.theme_builder_title_edit
import campfire.ui.theming.ui.generated.resources.theme_builder_title_new
import campfire.ui.theming.ui.generated.resources.theme_fine_tune_dark_mode
import campfire.ui.theming.ui.generated.resources.theme_save_default_name
import campfire.ui.theming.ui.generated.resources.theme_tune_mirrored_to_dark
import campfire.ui.theming.ui.generated.resources.theme_tune_mirrored_to_light
import com.r0adkll.swatchbuckler.color.dynamiccolor.Variant
import com.slack.circuit.codegen.annotations.CircuitInject
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Wide enough for the controls beside a light and a dark preview. */
private val TwoPaneMinWidth = 840.dp
private val ControlsPaneWidth = 400.dp

/**
 * The custom theme builder. A preview of Campfire's screens stays on screen while the controls
 * below it (or beside it, on wide layouts) change the theme. Picking a color, or fine-tuning a
 * part of the app, swaps the controls for a page of its own so the preview stays visible.
 *
 * The builder's own controls draw with the generated colors, and only the preview shows
 * fine-tunes, so a fine-tune can't make the builder itself hard to use.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@CircuitInject(ThemeBuilderScreen::class, UserScope::class)
@Composable
fun ThemeBuilder(
  state: ThemeBuilderUiState,
  modifier: Modifier = Modifier,
) {
  val systemDarkMode = LocalUseDarkColors.current
  var isDarkMode by rememberSaveable { mutableStateOf(systemDarkMode) }
  // Side by side, both modes show at once, so fine-tuning keeps a mode of its own.
  var tuneDarkMode by rememberSaveable { mutableStateOf(systemDarkMode) }
  var scene by rememberSaveable { mutableStateOf(PreviewScene.Home) }
  var pages by rememberSaveable(stateSaver = ControlsPage.StackSaver) { mutableStateOf(emptyList<ControlsPage>()) }
  var showSaveSheet by rememberSaveable { mutableStateOf(false) }
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  val mirroredToDark = stringResource(Res.string.theme_tune_mirrored_to_dark)
  val mirroredToLight = stringResource(Res.string.theme_tune_mirrored_to_light)

  OverlayPriorityBackHandler(enabled = pages.isNotEmpty()) { pages = pages.dropLast(1) }

  BoxWithConstraints(modifier) {
    val twoPane = maxWidth >= TwoPaneMinWidth
    val chromeDarkMode = if (twoPane) systemDarkMode else isDarkMode
    val previewHeight = (maxHeight * 0.42f).coerceIn(260.dp, 380.dp)
    val editDarkMode = if (twoPane) tuneDarkMode else isDarkMode
    val onEditDarkModeChange: (Boolean) -> Unit = { if (twoPane) tuneDarkMode = it else isDarkMode = it }
    val onTune: (ContrastCheck, Boolean) -> Unit = { check, dark ->
      onEditDarkModeChange(dark)
      pages = listOf(ControlsPage.Tune(check, check.background))
    }

    MaterialExpressiveTheme(colorScheme = paletteColorScheme(state.generatedPalette, chromeDarkMode, false)) {
      Scaffold(
        topBar = {
          ThemeBuilderTopBar(
            state = state,
            showModeSwitch = !twoPane,
            isDarkMode = isDarkMode,
            onDarkModeChange = { isDarkMode = it },
            onSaveClick = { showSaveSheet = true },
          )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentWindowInsets = CampfireWindowInsets,
      ) { padding ->
        val controls = @Composable { controlsModifier: Modifier ->
          ThemeBuilderControls(
            state = state,
            page = pages.lastOrNull(),
            chromeDarkMode = chromeDarkMode,
            editDarkMode = editDarkMode,
            showModeSwitch = twoPane,
            onEditDarkModeChange = onEditDarkModeChange,
            onOpen = { pages = pages + it },
            onReplace = { pages = pages.dropLast(1) + it },
            onClose = { pages = pages.dropLast(1) },
            onTune = onTune,
            onMirrored = { toDark ->
              scope.launch { snackbarHostState.showSnackbar(if (toDark) mirroredToDark else mirroredToLight) }
            },
            modifier = controlsModifier,
          )
        }

        if (twoPane) {
          Row(
            Modifier
              .fillMaxSize()
              .padding(padding),
          ) {
            controls(
              Modifier
                .width(ControlsPaneWidth)
                .fillMaxHeight(),
            )
            LightAndDarkPreview(
              state = state,
              scene = scene,
              onSceneChange = { scene = it },
              onPartClick = onTune,
              modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 8.dp, end = 24.dp, bottom = 24.dp),
            )
          }
        } else {
          Column(
            Modifier
              .fillMaxSize()
              .padding(padding),
          ) {
            PreviewPane(
              state = state,
              scene = scene,
              onSceneChange = { scene = it },
              isDarkMode = isDarkMode,
              onPartClick = { onTune(it, isDarkMode) },
              modifier = Modifier
                .fillMaxWidth()
                .height(previewHeight)
                .padding(horizontal = 16.dp),
            )
            controls(Modifier.weight(1f))
          }
        }
      }

      if (showSaveSheet) {
        SaveThemeSheet(
          name = state.name,
          icon = state.theme.icon,
          suggestedName = stringResource(Res.string.theme_save_default_name, state.customThemeCount + 1),
          onIconClick = { state.eventSink(ThemeBuilderUiEvent.IconPicked(it)) },
          onSaveClick = {
            showSaveSheet = false
            state.eventSink(ThemeBuilderUiEvent.Save)
          },
          onDismissRequest = { showSaveSheet = false },
        )
      }
    }
  }
}

@Composable
private fun ThemeBuilderTopBar(
  state: ThemeBuilderUiState,
  showModeSwitch: Boolean,
  isDarkMode: Boolean,
  onDarkModeChange: (Boolean) -> Unit,
  onSaveClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  CampfireTopAppBar(
    title = {
      Text(stringResource(if (state.isNew) Res.string.theme_builder_title_new else Res.string.theme_builder_title_edit))
    },
    navigationIcon = {
      NavigationBackButton(onClick = { state.eventSink(ThemeBuilderUiEvent.Back) })
    },
    actions = {
      if (showModeSwitch) {
        DarkModeSwitch(
          isDarkMode = isDarkMode,
          onDarkModeChange = onDarkModeChange,
          modifier = Modifier.padding(end = 8.dp),
        )
      }
      if (!state.isNew) {
        DeleteMenu(onDeleteConfirm = { state.eventSink(ThemeBuilderUiEvent.Delete) })
      }
      Button(
        onClick = onSaveClick,
        modifier = Modifier.padding(end = 8.dp),
      ) {
        Text(stringResource(Res.string.theme_builder_save))
      }
    },
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    modifier = modifier,
  )
}

@Composable
private fun DeleteMenu(
  onDeleteConfirm: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var expanded by remember { mutableStateOf(false) }
  var confirming by remember { mutableStateOf(false) }
  val moreLabel = stringResource(Res.string.action_more_options)
  val deleteLabel = stringResource(Res.string.action_delete_theme)

  IconButtonTooltip(text = moreLabel, modifier = modifier) {
    IconButton(onClick = { expanded = true }) {
      Icon(CampfireIcons.Rounded.MoreVert, contentDescription = moreLabel)
    }
  }
  DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
    DropdownMenuItem(
      text = { Text(deleteLabel) },
      leadingIcon = { Icon(CampfireIcons.Rounded.Delete, contentDescription = null) },
      onClick = {
        expanded = false
        confirming = true
      },
    )
  }

  if (confirming) {
    AlertDialog(
      onDismissRequest = { confirming = false },
      title = { Text(stringResource(Res.string.theme_builder_delete_title)) },
      text = { Text(stringResource(Res.string.theme_builder_delete_body)) },
      confirmButton = {
        TextButton(
          onClick = {
            confirming = false
            onDeleteConfirm()
          },
        ) {
          Text(stringResource(Res.string.theme_builder_delete_confirm), color = MaterialTheme.colorScheme.error)
        }
      },
      dismissButton = {
        TextButton(onClick = { confirming = false }) {
          Text(stringResource(Res.string.theme_builder_cancel))
        }
      },
    )
  }
}

@Composable
private fun ThemeBuilderControls(
  state: ThemeBuilderUiState,
  page: ControlsPage?,
  chromeDarkMode: Boolean,
  editDarkMode: Boolean,
  showModeSwitch: Boolean,
  onEditDarkModeChange: (Boolean) -> Unit,
  onOpen: (ControlsPage) -> Unit,
  onReplace: (ControlsPage) -> Unit,
  onClose: () -> Unit,
  onTune: (ContrastCheck, Boolean) -> Unit,
  onMirrored: (toDark: Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  AnimatedContent(
    targetState = page,
    contentKey = { it?.contentKey },
    modifier = modifier,
  ) { shown ->
    val scheme = state.theme.colorPalette.scheme(editDarkMode)
    val overrides = state.roleOverrides.forMode(editDarkMode)
    when (shown) {
      null -> ControlsList(
        state = state,
        chromeDarkMode = chromeDarkMode,
        editDarkMode = editDarkMode,
        showModeSwitch = showModeSwitch,
        onEditDarkModeChange = onEditDarkModeChange,
        onPick = { onOpen(ControlsPage.Pick(it)) },
        onTune = onTune,
        onAllColorsClick = { onOpen(ControlsPage.AllColors) },
      )

      is ControlsPage.Pick -> {
        val row = state.palettes.first { it.key == shown.key }
        ColorPickerPanel(
          key = shown.key,
          initialColor = row.keyColor,
          seedColor = state.seedColor,
          canReset = row.isCustom,
          onColorChange = { state.eventSink(ThemeBuilderUiEvent.ColorPicked(shown.key, it)) },
          onResetClick = {
            state.eventSink(ThemeBuilderUiEvent.ColorReset(shown.key))
            onClose()
          },
          onDoneClick = onClose,
          modifier = Modifier.fillMaxSize(),
        )
      }

      is ControlsPage.Tune -> TunePanel(
        check = shown.check,
        role = shown.role,
        scheme = scheme,
        overrides = overrides,
        dark = editDarkMode,
        showModeSwitch = showModeSwitch,
        onDarkChange = onEditDarkModeChange,
        onRoleChange = { onReplace(shown.copy(role = it)) },
        onColorChange = { state.eventSink(ThemeBuilderUiEvent.RoleColorPicked(shown.role, editDarkMode, it)) },
        onFixContrastClick = {
          shown.check?.let { state.eventSink(ThemeBuilderUiEvent.FixContrast(it, editDarkMode)) }
        },
        onResetClick = { state.eventSink(ThemeBuilderUiEvent.RolesReset(shown.roles.toSet(), editDarkMode)) },
        onMirrorClick = {
          state.eventSink(ThemeBuilderUiEvent.MirrorRoles(shown.roles.toSet(), editDarkMode, shown.check))
          onMirrored(!editDarkMode)
        },
        onDoneClick = onClose,
        modifier = Modifier.fillMaxSize(),
      )

      ControlsPage.AllColors -> AllColorsPanel(
        scheme = scheme,
        overrides = overrides,
        dark = editDarkMode,
        onRoleClick = { onOpen(ControlsPage.Tune(check = null, role = it)) },
        onDoneClick = onClose,
        modifier = Modifier.fillMaxSize(),
      )
    }
  }
}

@Composable
private fun ControlsList(
  state: ThemeBuilderUiState,
  chromeDarkMode: Boolean,
  editDarkMode: Boolean,
  showModeSwitch: Boolean,
  onEditDarkModeChange: (Boolean) -> Unit,
  onPick: (PaletteKey) -> Unit,
  onTune: (ContrastCheck, Boolean) -> Unit,
  onAllColorsClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(24.dp),
  ) {
    ControlSection(Res.string.theme_builder_section_color) {
      SeedColorSection(
        seedColor = state.seedColor,
        onChangeClick = { onPick(PaletteKey.Accent) },
        onSwatchClick = { state.eventSink(ThemeBuilderUiEvent.ColorPicked(PaletteKey.Accent, it)) },
      )
    }
    ControlSection(Res.string.theme_builder_section_style) {
      StylePicker(
        styles = state.styles,
        selected = state.colorStyle,
        isDarkMode = chromeDarkMode,
        onStyleClick = { state.eventSink(ThemeBuilderUiEvent.StyleClick(it)) },
      )
    }
    ControlSection(Res.string.theme_builder_section_contrast) {
      ContrastPicker(
        level = state.contrastLevel,
        onLevelClick = { state.eventSink(ThemeBuilderUiEvent.ContrastLevelClick(it)) },
      )
    }
    ControlSection(Res.string.theme_builder_section_readability) {
      ReadabilitySummary(
        contrast = state.contrast,
        onCheckClick = { status ->
          // Open it in a mode where it fails, preferring the one being edited.
          onTune(status.check, if (status.passes(editDarkMode)) !editDarkMode else editDarkMode)
        },
      )
    }
    ControlSection(Res.string.theme_builder_section_palettes) {
      PaletteRows(
        palettes = state.palettes,
        onPaletteClick = onPick,
        onResetClick = { state.eventSink(ThemeBuilderUiEvent.ColorReset(it)) },
      )
    }
    ControlSection(
      title = Res.string.theme_builder_section_fine_tune,
      trailing = if (showModeSwitch) {
        {
          DarkModeSwitch(
            isDarkMode = editDarkMode,
            onDarkModeChange = onEditDarkModeChange,
            label = stringResource(Res.string.theme_fine_tune_dark_mode),
          )
        }
      } else {
        null
      },
    ) {
      FineTuneParts(
        overrides = state.roleOverrides.forMode(editDarkMode),
        failing = state.failingChecks(editDarkMode),
        dark = editDarkMode,
        onPartClick = { onTune(it, editDarkMode) },
        onAllColorsClick = onAllColorsClick,
      )
    }
  }
}

@Composable
private fun ControlSection(
  title: StringResource,
  modifier: Modifier = Modifier,
  trailing: (@Composable () -> Unit)? = null,
  content: @Composable () -> Unit,
) {
  Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        text = stringResource(title),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.weight(1f),
      )
      trailing?.invoke()
    }
    content()
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PreviewPane(
  state: ThemeBuilderUiState,
  scene: PreviewScene,
  onSceneChange: (PreviewScene) -> Unit,
  isDarkMode: Boolean,
  onPartClick: (ContrastCheck) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
    PreviewSceneTabs(scene = scene, onSceneChange = onSceneChange)
    MaterialExpressiveTheme(colorScheme = colorScheme(state.theme, isDarkMode)) {
      ThemePreviewContent(
        scene = scene,
        content = state.previewContent,
        icon = state.theme.icon,
        targets = ThemeTargets(failing = state.failingChecks(isDarkMode), onClick = onPartClick),
        onCoverThemingClick = { state.eventSink(ThemeBuilderUiEvent.CoverThemingSettings) },
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
      )
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LightAndDarkPreview(
  state: ThemeBuilderUiState,
  scene: PreviewScene,
  onSceneChange: (PreviewScene) -> Unit,
  onPartClick: (ContrastCheck, dark: Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
    PreviewSceneTabs(
      scene = scene,
      onSceneChange = onSceneChange,
      modifier = Modifier.widthIn(max = 480.dp),
    )
    Row(
      modifier = Modifier.weight(1f),
      horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      listOf(false, true).forEach { dark ->
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = stringResource(
              if (dark) Res.string.theme_builder_preview_dark else Res.string.theme_builder_preview_light,
            ),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          MaterialExpressiveTheme(colorScheme = colorScheme(state.theme, dark)) {
            ThemePreviewContent(
              scene = scene,
              content = state.previewContent,
              icon = state.theme.icon,
              targets = ThemeTargets(failing = state.failingChecks(dark), onClick = { onPartClick(it, dark) }),
              onCoverThemingClick = { state.eventSink(ThemeBuilderUiEvent.CoverThemingSettings) },
              modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            )
          }
        }
      }
    }
  }
}

/** A sample builder state for previews and tests. */
@Composable
internal fun rememberSampleThemeBuilderState(
  colors: CustomThemeColors = CustomThemeColors(Color(0xFFC0532D), Variant.EXPRESSIVE),
  isNew: Boolean = true,
  previewContent: PreviewContent = PreviewContent(),
  eventSink: (ThemeBuilderUiEvent) -> Unit = {},
): ThemeBuilderUiState {
  val name = rememberTextFieldState(if (isNew) "" else "Ember")
  val palette = remember(colors) { colors.toColorPalette() }
  val generatedPalette = remember(colors) { colors.toGeneratedPalette() }
  return ThemeBuilderUiState(
    theme = AppTheme.Fixed.Custom(
      id = if (isNew) NEW_THEME_ID else "ember",
      name = name.text.toString(),
      seedColor = colors.seedColor,
      colorSpec = colors.colorSpec,
      colorStyle = colors.colorStyle,
      contrastLevel = colors.contrastLevel.toFloat(),
      secondaryColorOverride = colors.secondaryColor,
      tertiaryColorOverride = colors.tertiaryColor,
      errorColorOverride = colors.errorColor,
      neutralColorOverride = colors.neutralColor,
      neutralVariantColorOverride = colors.neutralVariantColor,
      icon = AppTheme.Icon.Campfire,
      colorPalette = palette,
      roleOverrides = colors.roleOverrides,
    ),
    generatedPalette = generatedPalette,
    name = name,
    styles = remember(colors) { styleOptions(colors) },
    palettes = remember(colors) { paletteRows(colors) },
    contrast = remember(palette) { contrastStatuses(palette) },
    customThemeCount = 2,
    previewContent = previewContent,
    eventSink = eventSink,
  )
}

@Preview
@Composable
fun ThemeBuilderPreview() {
  CompositionLocalProvider(
    LocalWindowSizeClass provides currentWindowSizeClass(),
    LocalContentLayout provides ContentLayout.Root,
  ) {
    ThemeBuilder(state = rememberSampleThemeBuilderState())
  }
}
