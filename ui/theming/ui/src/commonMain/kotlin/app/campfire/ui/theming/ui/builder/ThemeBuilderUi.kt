// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.builder

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import app.campfire.common.compose.CampfireWindowInsets
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.back.OverlayPriorityBackHandler
import app.campfire.common.compose.currentWindowSizeClass
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.CloseFullscreen
import app.campfire.common.compose.icons.rounded.Delete
import app.campfire.common.compose.icons.rounded.MoreVert
import app.campfire.common.compose.icons.rounded.OpenInFull
import app.campfire.common.compose.layout.ContentLayout
import app.campfire.common.compose.layout.LocalContentLayout
import app.campfire.common.compose.session.LocalPlayerDocked
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
import app.campfire.ui.theming.ui.builder.composables.BuilderToolbar
import app.campfire.ui.theming.ui.builder.composables.ColorPickerPanel
import app.campfire.ui.theming.ui.builder.composables.CompactSeedColorSection
import app.campfire.ui.theming.ui.builder.composables.ContrastPicker
import app.campfire.ui.theming.ui.builder.composables.FineTuneParts
import app.campfire.ui.theming.ui.builder.composables.FineTuneTray
import app.campfire.ui.theming.ui.builder.composables.PaletteRows
import app.campfire.ui.theming.ui.builder.composables.PaletteTiles
import app.campfire.ui.theming.ui.builder.composables.ReadabilityChip
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
import app.campfire.ui.theming.ui.preview.scenes
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
import campfire.ui.theming.ui.generated.resources.theme_builder_section_style
import campfire.ui.theming.ui.generated.resources.theme_builder_show_controls
import campfire.ui.theming.ui.generated.resources.theme_builder_show_whole_preview
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

/** How much of the tray's height a page may take, of what the preview and tray share. */
private const val TrayPageFraction = 0.6f
private val MaxTrayPageHeight = 360.dp

/** The preview's header row and the toolbar, around the preview and tray. */
private val TrayChromeHeight = 120.dp

/**
 * The custom theme builder. A preview of Campfire's screens stays on screen while the controls
 * change the theme, laid out for the space there is (see [builderLayout]):
 * - On a phone, the controls are a tray under the preview that shows one tool at a time, picked
 *   from a toolbar. The tray can hide, to give the preview the screen.
 * - On a foldable half-open on a table, the preview sits above the hinge and the tray below it.
 * - In wider windows, every control sits in a column beside the preview, lined up with a fold
 *   that divides the screen, and, with room for both, beside light and dark previews at once.
 *
 * Picking a color or fine-tuning a part opens a page in place of the tool or the column, so the
 * preview stays visible, and fine-tuning zooms the preview in on the part.
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
  // Beside light and dark previews, both modes show at once, so fine-tuning keeps a mode of its own.
  var tuneDarkMode by rememberSaveable { mutableStateOf(systemDarkMode) }
  var scene by rememberSaveable { mutableStateOf(PreviewScene.Home) }
  var pages by rememberSaveable(stateSaver = ControlsPage.StackSaver) { mutableStateOf(emptyList<ControlsPage>()) }
  var tool by rememberSaveable { mutableStateOf(BuilderTool.Color) }
  var showTray by rememberSaveable { mutableStateOf(true) }
  var zoomed by rememberSaveable { mutableStateOf(true) }
  var showSaveSheet by rememberSaveable { mutableStateOf(false) }
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  val mirroredToDark = stringResource(Res.string.theme_tune_mirrored_to_dark)
  val mirroredToLight = stringResource(Res.string.theme_tune_mirrored_to_light)

  // Where the content sits in the window, to place a fold that divides it.
  var contentBounds by remember { mutableStateOf(Rect.Zero) }
  val posture = currentWindowAdaptiveInfoV2().windowPosture
  val density = LocalDensity.current
  val playerDocked = LocalPlayerDocked.current
  val hinge = remember(posture, contentBounds, density, playerDocked) {
    foldHinge(posture, contentBounds, density, playerDocked)
  }

  BoxWithConstraints(modifier) {
    val contentSize = if (contentBounds.isEmpty) {
      DpSize(maxWidth, maxHeight)
    } else {
      with(density) { DpSize(contentBounds.width.toDp(), contentBounds.height.toDp()) }
    }
    val layout = builderLayout(contentSize, hinge)
    val dual = layout is BuilderLayout.Side && layout.dual
    val chromeDarkMode = if (dual) systemDarkMode else isDarkMode
    val editDarkMode = if (dual) tuneDarkMode else isDarkMode
    val onEditDarkModeChange: (Boolean) -> Unit = { if (dual) tuneDarkMode = it else isDarkMode = it }
    val onTune: (ContrastCheck, Boolean) -> Unit = { check, dark ->
      onEditDarkModeChange(dark)
      pages = listOf(ControlsPage.Tune(check, check.background))
      tool = BuilderTool.FineTune
      showTray = true
      if (scene !in check.scenes) scene = check.scenes.first()
    }
    val hasFailingChecks = state.contrast.any { !it.passesBoth }
    val onReadabilityClick: () -> Unit = {
      val failing = state.contrast.filterNot { it.passesBoth }
      // Open the first part that's hard to read, in the mode being edited if it fails there.
      val status = failing.firstOrNull { !it.passes(editDarkMode) } ?: failing.firstOrNull()
      if (status != null) {
        onTune(status.check, if (status.passes(editDarkMode)) !editDarkMode else editDarkMode)
      } else {
        tool = BuilderTool.FineTune
        pages = emptyList()
        showTray = true
      }
    }
    val trayHidden = !showTray && layout == BuilderLayout.Tray
    // Hiding the tray asks for the whole preview, so it zooms out too.
    val focus = (pages.lastOrNull() as? ControlsPage.Tune)?.check?.takeIf { zoomed && !trayHidden }

    // Back brings a hidden tray back first, so it never closes a page the user can't see.
    OverlayPriorityBackHandler(enabled = pages.isNotEmpty() || trayHidden) {
      if (trayHidden) showTray = true else pages = pages.dropLast(1)
    }

    MaterialExpressiveTheme(colorScheme = paletteColorScheme(state.generatedPalette, chromeDarkMode, false)) {
      Scaffold(
        topBar = {
          ThemeBuilderTopBar(
            state = state,
            showModeSwitch = !dual,
            isDarkMode = isDarkMode,
            onDarkModeChange = { isDarkMode = it },
            onSaveClick = { showSaveSheet = true },
          )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentWindowInsets = CampfireWindowInsets,
      ) { padding ->
        val controls: @Composable (
          idleKey: String,
          pageModifier: Modifier,
          controlsModifier: Modifier,
          idle: @Composable (idleKey: String) -> Unit,
        ) -> Unit = { idleKey, pageModifier, controlsModifier, idle ->
          ThemeBuilderControls(
            state = state,
            page = pages.lastOrNull(),
            idleKey = idleKey,
            editDarkMode = editDarkMode,
            showModeSwitch = dual,
            zoomed = zoomed,
            onZoomChange = { zoomed = it },
            onEditDarkModeChange = onEditDarkModeChange,
            onOpen = { pages = pages + it },
            onReplace = { pages = pages.dropLast(1) + it },
            onClose = { pages = pages.dropLast(1) },
            onMirrored = { toDark ->
              scope.launch { snackbarHostState.showSnackbar(if (toDark) mirroredToDark else mirroredToLight) }
            },
            pageModifier = pageModifier,
            modifier = controlsModifier,
            idle = idle,
          )
        }
        val toolTray: @Composable (BuilderTool, Modifier) -> Unit = { shown, trayModifier ->
          ToolTray(
            state = state,
            tool = shown,
            chromeDarkMode = chromeDarkMode,
            editDarkMode = editDarkMode,
            onPick = { pages = listOf(ControlsPage.Pick(it)) },
            onTune = { onTune(it, editDarkMode) },
            onAllColorsClick = { pages = listOf(ControlsPage.AllColors) },
            modifier = trayModifier,
          )
        }
        val onToolClick: (BuilderTool) -> Unit = {
          tool = it
          pages = emptyList()
          showTray = true
        }
        val previewHeader: @Composable (Modifier, Boolean) -> Unit = { headerModifier, offerExpand ->
          PreviewHeader(
            scene = scene,
            onSceneChange = { scene = it },
            contrast = state.contrast,
            onReadabilityClick = onReadabilityClick.takeIf { hasFailingChecks || layout !is BuilderLayout.Side },
            expanded = (!showTray).takeIf { offerExpand },
            onExpandChange = { showTray = !it },
            modifier = headerModifier,
          )
        }

        Box(
          Modifier
            .fillMaxSize()
            .padding(padding)
            // Not boundsInWindow, which clips to the window and so would misjudge the width.
            .onPlaced { contentBounds = Rect(it.positionInWindow(), it.size.toSize()) },
        ) {
          when (layout) {
            BuilderLayout.Tray -> BoxWithConstraints(Modifier.fillMaxSize()) {
              val pageHeight = ((maxHeight - TrayChromeHeight) * TrayPageFraction).coerceAtMost(MaxTrayPageHeight)
              Column(Modifier.fillMaxSize()) {
                previewHeader(Modifier.padding(horizontal = 16.dp), true)
                PreviewFrame(
                  state = state,
                  scene = scene,
                  dark = isDarkMode,
                  focus = focus,
                  onPartClick = { onTune(it, isDarkMode) },
                  modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                )
                AnimatedVisibility(
                  visible = showTray,
                  enter = expandVertically(),
                  exit = shrinkVertically(),
                ) {
                  controls(
                    tool.name,
                    Modifier
                      .fillMaxWidth()
                      .height(pageHeight),
                    Modifier.fillMaxWidth(),
                  ) { key ->
                    toolTray(
                      BuilderTool.valueOf(key),
                      Modifier
                        .fillMaxWidth()
                        .heightIn(max = pageHeight)
                        .verticalScroll(rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                    )
                  }
                }
                BuilderToolbar(
                  selected = tool.takeIf { showTray },
                  onToolClick = onToolClick,
                  modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .widthIn(max = ToolbarMaxWidth)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                )
              }
            }

            is BuilderLayout.Tabletop -> Column(Modifier.fillMaxSize()) {
              Column(
                Modifier
                  .fillMaxWidth()
                  .height(layout.topHeight),
              ) {
                previewHeader(Modifier.padding(horizontal = 16.dp), false)
                PreviewFrame(
                  state = state,
                  scene = scene,
                  dark = isDarkMode,
                  focus = focus,
                  onPartClick = { onTune(it, isDarkMode) },
                  modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                )
              }
              Spacer(Modifier.height(layout.hingeHeight))
              Column(
                modifier = Modifier
                  .weight(1f)
                  .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                controls(
                  tool.name,
                  Modifier.fillMaxSize(),
                  Modifier
                    .weight(1f)
                    .widthIn(max = TabletopTrayMaxWidth)
                    .fillMaxWidth(),
                ) { key ->
                  toolTray(
                    BuilderTool.valueOf(key),
                    Modifier
                      .fillMaxSize()
                      .verticalScroll(rememberScrollState())
                      .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                  )
                }
                BuilderToolbar(
                  selected = tool,
                  onToolClick = onToolClick,
                  modifier = Modifier
                    .widthIn(max = ToolbarMaxWidth)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                )
              }
            }

            is BuilderLayout.Side -> Row(Modifier.fillMaxSize()) {
              controls(
                "list",
                Modifier.fillMaxSize(),
                Modifier
                  .width(layout.controlsWidth)
                  .fillMaxHeight(),
              ) {
                ControlsList(
                  state = state,
                  chromeDarkMode = chromeDarkMode,
                  editDarkMode = editDarkMode,
                  showModeSwitch = dual,
                  onEditDarkModeChange = onEditDarkModeChange,
                  onPick = { pages = pages + ControlsPage.Pick(it) },
                  onTune = onTune,
                  onAllColorsClick = { pages = pages + ControlsPage.AllColors },
                )
              }
              Spacer(Modifier.width(layout.gap))
              Column(
                modifier = Modifier
                  .weight(1f)
                  .fillMaxHeight()
                  .padding(end = SidePreviewEndPadding, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                previewHeader(Modifier, false)
                if (layout.dual) {
                  LightAndDarkPreview(
                    state = state,
                    scene = scene,
                    focus = focus,
                    onPartClick = onTune,
                    modifier = Modifier.weight(1f),
                  )
                } else {
                  PreviewFrame(
                    state = state,
                    scene = scene,
                    dark = isDarkMode,
                    focus = focus,
                    onPartClick = { onTune(it, isDarkMode) },
                    modifier = Modifier
                      .weight(1f)
                      .fillMaxWidth(),
                  )
                }
              }
            }
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

private val ToolbarMaxWidth = 480.dp
private val TabletopTrayMaxWidth = 640.dp

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

/**
 * The open [page], or when there's none, the [idle] controls: the column of every control, or a
 * tool's tray, which [idleKey] names so a change of tool animates like a change of page.
 */
@Composable
private fun ThemeBuilderControls(
  state: ThemeBuilderUiState,
  page: ControlsPage?,
  idleKey: String,
  editDarkMode: Boolean,
  showModeSwitch: Boolean,
  zoomed: Boolean,
  onZoomChange: (Boolean) -> Unit,
  onEditDarkModeChange: (Boolean) -> Unit,
  onOpen: (ControlsPage) -> Unit,
  onReplace: (ControlsPage) -> Unit,
  onClose: () -> Unit,
  onMirrored: (toDark: Boolean) -> Unit,
  pageModifier: Modifier,
  modifier: Modifier = Modifier,
  idle: @Composable (idleKey: String) -> Unit,
) {
  AnimatedContent(
    targetState = page to idleKey,
    contentKey = { (shown, key) -> shown?.contentKey ?: "idle:$key" },
    modifier = modifier,
  ) { (shown, key) ->
    val scheme = state.theme.colorPalette.scheme(editDarkMode)
    val overrides = state.roleOverrides.forMode(editDarkMode)
    when (shown) {
      null -> idle(key)

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
          modifier = pageModifier,
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
        zoomed = zoomed,
        onZoomChange = onZoomChange,
        modifier = pageModifier,
      )

      ControlsPage.AllColors -> AllColorsPanel(
        scheme = scheme,
        overrides = overrides,
        dark = editDarkMode,
        onRoleClick = { onOpen(ControlsPage.Tune(check = null, role = it)) },
        onDoneClick = onClose,
        modifier = pageModifier,
      )
    }
  }
}

/** One tool's controls, laid out to fit the tray. */
@Composable
private fun ToolTray(
  state: ThemeBuilderUiState,
  tool: BuilderTool,
  chromeDarkMode: Boolean,
  editDarkMode: Boolean,
  onPick: (PaletteKey) -> Unit,
  onTune: (ContrastCheck) -> Unit,
  onAllColorsClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
    when (tool) {
      BuilderTool.Color -> CompactSeedColorSection(
        seedColor = state.seedColor,
        coverColors = state.coverColors,
        onChangeClick = { onPick(PaletteKey.Accent) },
        onSwatchClick = { state.eventSink(ThemeBuilderUiEvent.ColorPicked(PaletteKey.Accent, it)) },
      )

      BuilderTool.Style -> {
        StylePicker(
          styles = state.styles,
          selected = state.colorStyle,
          isDarkMode = chromeDarkMode,
          onStyleClick = { state.eventSink(ThemeBuilderUiEvent.StyleClick(it)) },
        )
        ControlSection(Res.string.theme_builder_section_contrast) {
          ContrastPicker(
            level = state.contrastLevel,
            onLevelClick = { state.eventSink(ThemeBuilderUiEvent.ContrastLevelClick(it)) },
          )
        }
      }

      BuilderTool.Palette -> PaletteTiles(
        palettes = state.palettes,
        onPaletteClick = onPick,
      )

      BuilderTool.FineTune -> FineTuneTray(
        overrides = state.roleOverrides.forMode(editDarkMode),
        failing = state.failingChecks(editDarkMode),
        dark = editDarkMode,
        onPartClick = onTune,
        onAllColorsClick = onAllColorsClick,
      )
    }
  }
}

/** Every control in one column, for windows wide enough to put it beside the preview. */
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
        coverColors = state.coverColors,
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

/**
 * The row over the preview: the scene tabs, whether the theme is readable, and, when [expanded]
 * isn't null, a button that hides the controls to show the whole preview.
 */
@Composable
private fun PreviewHeader(
  scene: PreviewScene,
  onSceneChange: (PreviewScene) -> Unit,
  contrast: List<ContrastStatus>,
  onReadabilityClick: (() -> Unit)?,
  modifier: Modifier = Modifier,
  expanded: Boolean? = null,
  onExpandChange: (Boolean) -> Unit = {},
) {
  BoxWithConstraints(modifier.fillMaxWidth()) {
    // On a phone the tabs need the chip's words to fit their own.
    val compactChip = maxWidth < CompactHeaderWidth
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 48.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Box(Modifier.weight(1f)) {
        PreviewSceneTabs(
          scene = scene,
          onSceneChange = onSceneChange,
          modifier = Modifier.widthIn(max = SceneTabsMaxWidth),
        )
      }
      ReadabilityChip(contrast = contrast, onClick = onReadabilityClick, compact = compactChip)
      if (expanded != null) {
        val label = stringResource(
          if (expanded) Res.string.theme_builder_show_controls else Res.string.theme_builder_show_whole_preview,
        )
        IconButtonTooltip(text = label) {
          IconButton(onClick = { onExpandChange(!expanded) }) {
            Icon(
              if (expanded) CampfireIcons.Rounded.CloseFullscreen else CampfireIcons.Rounded.OpenInFull,
              contentDescription = label,
            )
          }
        }
      }
    }
  }
}

private val SceneTabsMaxWidth = 480.dp
private val CompactHeaderWidth = 440.dp

/** The preview in one mode, its failing parts outlined, zoomed in on [focus] while it's set. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PreviewFrame(
  state: ThemeBuilderUiState,
  scene: PreviewScene,
  dark: Boolean,
  focus: ContrastCheck?,
  onPartClick: (ContrastCheck) -> Unit,
  modifier: Modifier = Modifier,
) {
  MaterialExpressiveTheme(colorScheme = colorScheme(state.theme, dark)) {
    ThemePreviewContent(
      scene = scene,
      content = state.previewContent,
      icon = state.theme.icon,
      targets = ThemeTargets(failing = state.failingChecks(dark), onClick = onPartClick),
      focus = focus,
      onCoverThemingClick = { state.eventSink(ThemeBuilderUiEvent.CoverThemingSettings) },
      modifier = modifier,
    )
  }
}

/** Light and dark previews side by side, both zoomed in on [focus] while it's set. */
@Composable
private fun LightAndDarkPreview(
  state: ThemeBuilderUiState,
  scene: PreviewScene,
  focus: ContrastCheck?,
  onPartClick: (ContrastCheck, dark: Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(DualPreviewGap),
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
        PreviewFrame(
          state = state,
          scene = scene,
          dark = dark,
          focus = focus,
          onPartClick = { onPartClick(it, dark) },
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        )
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
  coverColors: List<CoverColor> = emptyList(),
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
    coverColors = coverColors,
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
