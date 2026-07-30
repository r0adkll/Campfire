// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ai.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.LibraryBooks
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallExtendedFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Dummy mock-up of the Campfire app surface used to preview a generated theme. Renders a
 * faux top app bar, two list cards, a floating action button, and a bottom nav bar — all
 * driven entirely by the surrounding [MaterialTheme]'s color scheme.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun AppPreview(
  isDarkMode: Boolean,
  onDarkModeChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    color = MaterialTheme.colorScheme.background,
    contentColor = MaterialTheme.colorScheme.onBackground,
    shape = RoundedCornerShape(24.dp),
    tonalElevation = 1.dp,
    shadowElevation = 3.dp,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
    ) {
      PreviewTopBar(
        isDarkMode = isDarkMode,
        onDarkModeChange = onDarkModeChange,
      )
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        PreviewItemCard(
          title = "Primary color",
          subtitle = "Color used for main ui elements",
          progress = 0.62f,
          accent = MaterialTheme.colorScheme.primary,
          accentContainer = MaterialTheme.colorScheme.primaryContainer,
          onAccentContainer = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        PreviewItemCard(
          title = "Tertiary color",
          subtitle = "Color used for distinct accents",
          progress = 0.18f,
          accent = MaterialTheme.colorScheme.tertiary,
          accentContainer = MaterialTheme.colorScheme.tertiaryContainer,
          onAccentContainer = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        PreviewActionRow()
      }
      PreviewBottomBar()
    }
  }
}

@Composable
private fun PreviewTopBar(
  isDarkMode: Boolean,
  onDarkModeChange: (Boolean) -> Unit,
) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor = MaterialTheme.colorScheme.onSurface,
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier = Modifier
          .size(28.dp)
          .clip(CircleShape)
          .padding(2.dp),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          Icons.Filled.Headphones,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
        )
      }
      Spacer(Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "Library",
          style = MaterialTheme.typography.titleLarge,
        )
        Text(
          text = "AI-generated theme preview",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Switch(
        checked = isDarkMode,
        onCheckedChange = onDarkModeChange,
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
    }
  }
}

@Composable
private fun PreviewItemCard(
  title: String,
  subtitle: String,
  progress: Float,
  accent: Color,
  accentContainer: Color,
  onAccentContainer: Color,
) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor = MaterialTheme.colorScheme.onSurface,
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(
        modifier = Modifier.size(56.dp),
        shape = RoundedCornerShape(14.dp),
        color = accentContainer,
        contentColor = onAccentContainer,
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            Icons.AutoMirrored.Rounded.LibraryBooks,
            contentDescription = null,
          )
        }
      }
      Spacer(Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape),
        ) {
          Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
          ) { Spacer(Modifier.height(6.dp)) }
          Surface(
            modifier = Modifier
              .fillMaxWidth(progress.coerceIn(0f, 1f))
              .height(6.dp),
            color = accent,
          ) { Spacer(Modifier.height(6.dp)) }
        }
      }
      Spacer(Modifier.width(12.dp))
      Surface(
        modifier = Modifier.size(36.dp),
        shape = CircleShape,
        color = accent,
        contentColor = MaterialTheme.colorScheme.onPrimary,
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(Icons.Rounded.PlayArrow, contentDescription = null)
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PreviewActionRow() {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    PreviewChip(label = "Sci-fi", filled = true)
    PreviewChip(label = "Mystery", filled = false)
    PreviewChip(label = "Memoir", filled = false)
    Spacer(Modifier.weight(1f))
    SmallExtendedFloatingActionButton(
      text = {
        Text("Secondary")
      },
      icon = {
        Icon(Icons.Rounded.Add, contentDescription = null)
      },
      containerColor = MaterialTheme.colorScheme.secondaryContainer,
      contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
      onClick = {},
    )
  }
}

@Composable
private fun PreviewChip(label: String, filled: Boolean) {
  val containerColor = if (filled) {
    MaterialTheme.colorScheme.primary
  } else {
    MaterialTheme.colorScheme.surfaceContainerHighest
  }
  val contentColor = if (filled) {
    MaterialTheme.colorScheme.onPrimary
  } else {
    MaterialTheme.colorScheme.onSurface
  }
  Surface(
    shape = RoundedCornerShape(50),
    color = containerColor,
    contentColor = contentColor,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
    )
  }
}

@Composable
private fun PreviewBottomBar() {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor = MaterialTheme.colorScheme.onSurface,
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(PaddingValues(horizontal = 16.dp, vertical = 10.dp)),
      horizontalArrangement = Arrangement.SpaceEvenly,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      PreviewBottomItem(Icons.Rounded.Home, "Home", selected = true)
      PreviewBottomItem(Icons.AutoMirrored.Rounded.LibraryBooks, "Library", selected = false)
      PreviewBottomItem(Icons.Rounded.Search, "Search", selected = false)
      PreviewBottomItem(Icons.Rounded.Person, "Profile", selected = false)
    }
  }
}

@Composable
private fun PreviewBottomItem(
  icon: ImageVector,
  label: String,
  selected: Boolean,
) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Surface(
      modifier = Modifier.size(width = 44.dp, height = 28.dp),
      shape = RoundedCornerShape(50),
      color = if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
      } else {
        MaterialTheme.colorScheme.surfaceContainer
      },
      contentColor = if (selected) {
        MaterialTheme.colorScheme.onSecondaryContainer
      } else {
        MaterialTheme.colorScheme.onSurfaceVariant
      },
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null)
      }
    }
    Spacer(Modifier.height(2.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = if (selected) {
        MaterialTheme.colorScheme.onSurface
      } else {
        MaterialTheme.colorScheme.onSurfaceVariant
      },
    )
  }
}
