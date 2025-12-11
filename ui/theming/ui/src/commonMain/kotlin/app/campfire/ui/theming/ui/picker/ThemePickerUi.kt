package app.campfire.ui.theming.ui.picker

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FormatPaint
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.CampfireWindowInsets
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.theme.Palette
import app.campfire.common.compose.widgets.CampfireTopAppBar
import app.campfire.core.coroutines.onError
import app.campfire.core.coroutines.onLoaded
import app.campfire.core.coroutines.onLoading
import app.campfire.core.di.UserScope
import app.campfire.ui.theming.api.AppTheme
import app.campfire.ui.theming.api.AppThemeImage
import app.campfire.ui.theming.api.colorScheme
import app.campfire.ui.theming.api.screen.ThemePickerScreen
import campfire.ui.theming.ui.generated.resources.Res
import campfire.ui.theming.ui.generated.resources.theme_name_dynamic
import campfire.ui.theming.ui.generated.resources.theme_name_forest
import campfire.ui.theming.ui.generated.resources.theme_name_life_float
import campfire.ui.theming.ui.generated.resources.theme_name_mountain
import campfire.ui.theming.ui.generated.resources.theme_name_rucksack
import campfire.ui.theming.ui.generated.resources.theme_name_tent
import campfire.ui.theming.ui.generated.resources.theme_name_water_bottle
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@CircuitInject(ThemePickerScreen::class, UserScope::class)
@Composable
fun ThemePicker(
  state: ThemePickerUiState,
  modifier: Modifier = Modifier,
) {
  MaterialExpressiveTheme(
    colorScheme = colorScheme(state.currentTheme),
  ) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
      topBar = {
        CampfireTopAppBar(
          title = {
            Text("Theme")
          },
          navigationIcon = {
            IconButton(
              onClick = { state.eventSink(ThemePickerUiEvent.Back) },
            ) {
              Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
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
        modifier = Modifier,
        contentPadding = paddingValues,
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        item {
          Text(
            text = "Pick a built-in theme or create your own custom one to personalize your Campfire experience.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(16.dp),
          )
        }

        items(state.builtInThemes) { theme ->
          ThemeOption(
            theme = theme,
            selected = theme == state.currentTheme,
            onClick = {
              state.eventSink(ThemePickerUiEvent.SelectTheme(theme))
            },
            modifier = Modifier.padding(horizontal = 16.dp),
          )
        }

        item {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 8.dp),
          ) {
            HorizontalDivider(Modifier.weight(1f))
            Text(
              text = "Custom".uppercase(),
              style = MaterialTheme.typography.titleMediumEmphasized,
              color = DividerDefaults.color,
              modifier = Modifier.padding(horizontal = 16.dp),
            )
            HorizontalDivider(Modifier.weight(1f))
          }
        }

        state.customThemes
          .onLoading {
            item {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(56.dp),
                contentAlignment = Alignment.Center,
              ) {
                CircularWavyProgressIndicator(
                  modifier = Modifier.size(40.dp),
                )
              }
            }
          }
          .onLoaded { customThemes ->
            items(customThemes) { customTheme ->
              ThemeOption(
                theme = customTheme,
                selected = customTheme.id == (state.currentTheme as? AppTheme.Fixed.Custom)?.id,
                onClick = {
                  state.eventSink(ThemePickerUiEvent.SelectTheme(customTheme))
                },
                onEditClick = {
                  state.eventSink(ThemePickerUiEvent.OpenThemeBuilder(customTheme))
                },
                modifier = Modifier.padding(horizontal = 16.dp),
              )
            }
          }
          .onError {
            item {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(56.dp)
                  .padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center,
              ) {
                Text("Uh-oh! Unable to load custom themes")
              }
            }
          }

        item {
          CreateCustomThemeOption(
            modifier = Modifier.padding(
              horizontal = 16.dp,
            ),
            onClick = {
              state.eventSink(ThemePickerUiEvent.OpenThemeBuilder())
            },
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterialApi::class)
@Composable
private fun ThemeOption(
  theme: AppTheme,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  onEditClick: (() -> Unit)? = null,
) {
  MaterialExpressiveTheme(
    colorScheme = colorScheme(theme),
  ) {
    val borderStrokeWidth = if (selected) 2.dp else 1.dp
    val cornerRadius by animateDpAsState(
      targetValue = if (selected) 40.dp else 12.dp,
      animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
    )
    Surface(
      modifier = modifier
        .fillMaxWidth(),
      shape = RoundedCornerShape(cornerRadius),
      color = MaterialTheme.colorScheme.primaryContainer,
      contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
      border = BorderStroke(borderStrokeWidth, MaterialTheme.colorScheme.primary),
      onClick = onClick,
    ) {
      Row(
        modifier = Modifier
          .padding(
            horizontal = 8.dp,
            vertical = 8.dp,
          ),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        // Theme Icon,
        Box(
          modifier = Modifier
            .background(
              color = MaterialTheme.colorScheme.surfaceContainerLow,
              shape = CircleShape,
            )
            .padding(6.dp),
        ) {
          AppThemeImage(
            appTheme = theme,
            modifier = Modifier.size(32.dp),
          )
        }

        Spacer(Modifier.size(16.dp))

        // Theme Name
        Text(
          text = when (theme) {
            is AppTheme.Fixed.Custom -> theme.name
            AppTheme.Fixed.Tent -> stringResource(Res.string.theme_name_tent)
            AppTheme.Fixed.Forest -> stringResource(Res.string.theme_name_forest)
            AppTheme.Fixed.WaterBottle -> stringResource(Res.string.theme_name_water_bottle)
            AppTheme.Fixed.Rucksack -> stringResource(Res.string.theme_name_rucksack)
            AppTheme.Fixed.LifeFloat -> stringResource(Res.string.theme_name_life_float)
            AppTheme.Fixed.Mountain -> stringResource(Res.string.theme_name_mountain)
            AppTheme.Dynamic -> stringResource(Res.string.theme_name_dynamic)
          },
          style = if (selected) {
            MaterialTheme.typography.titleMediumEmphasized
          } else {
            MaterialTheme.typography.titleMedium
          },
          color = MaterialTheme.colorScheme.onPrimaryContainer,
        )

        Spacer(Modifier.weight(1f))

        if (onEditClick != null) {
          IconButton(onClick = onEditClick) {
            Icon(
              Icons.Rounded.Edit,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
          }
          Spacer(Modifier.size(8.dp))
        }

        AnimatedPalettePreview(selected)
      }
    }
  }
}

@Composable
private fun AnimatedPalettePreview(
  selected: Boolean,
  modifier: Modifier = Modifier,
) {
  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier,
  ) {
    PalettePreview()

    AnimatedVisibility(
      visible = selected,
      enter = expandIn(
        expandFrom = Alignment.Center,
      ) + fadeIn(),
      exit = shrinkOut(
        shrinkTowards = Alignment.Center,
      ) + fadeOut(),
    ) {
      Icon(
        Icons.Rounded.FormatPaint,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.surface,
      )
    }
  }
}

@Composable
private fun PalettePreview(
  modifier: Modifier = Modifier,
) {
  val shape = CircleShape
  Column(
    modifier = modifier
      .size(48.dp)
      .clip(shape)
      .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Box(
      Modifier
        .weight(1f)
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.primary),
    )

    HorizontalDivider()

    Row(
      modifier = Modifier.weight(1f),
    ) {
      Box(
        Modifier
          .weight(1f)
          .fillMaxHeight()
          .background(MaterialTheme.colorScheme.secondary),
      )

      VerticalDivider()

      Box(
        Modifier
          .weight(1f)
          .fillMaxHeight()
          .background(MaterialTheme.colorScheme.tertiary),
      )
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterialApi::class)
@Composable
private fun CreateCustomThemeOption(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier
      .fillMaxWidth(),
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.surfaceContainerLowest,
    contentColor = MaterialTheme.colorScheme.onSurface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    onClick = onClick,
  ) {
    Row(
      modifier = Modifier
        .padding(
          horizontal = 8.dp,
          vertical = 8.dp,
        ),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // Theme Icon,
      Box(
        modifier = Modifier
          .background(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = CircleShape,
          )
          .padding(6.dp),
      ) {
        Image(
          CampfireIcons.Theme.Palette,
          contentDescription = null,
          modifier = Modifier
            .size(32.dp),
        )
      }

      Spacer(Modifier.size(16.dp))

      // Theme Name
      Text(
        text = "Create a custom theme",
        style = MaterialTheme.typography.titleMediumEmphasized,
      )
    }
  }
}
