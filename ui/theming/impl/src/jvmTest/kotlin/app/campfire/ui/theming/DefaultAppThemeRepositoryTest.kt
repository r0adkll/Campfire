// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming

import androidx.compose.ui.graphics.Color
import app.campfire.common.test.coroutines.asTestDispatcherProvider
import app.campfire.settings.api.ThemeKey
import app.campfire.settings.test.TestThemeSettings
import app.campfire.themes.CampfireThemeDatabase
import app.campfire.ui.theming.api.AppTheme
import app.campfire.ui.theming.api.RoleOverrides
import app.campfire.ui.theming.api.ThemeRole
import app.campfire.ui.theming.db.ThemingDatabaseFactory
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import com.r0adkll.swatchbuckler.color.dynamiccolor.ColorSpec
import com.r0adkll.swatchbuckler.color.dynamiccolor.Variant
import java.util.Properties
import kotlin.test.Test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

class DefaultAppThemeRepositoryTest {

  /** Foreign keys on, as Android opens the database, so replacing a theme row cascades. */
  private val driver = JdbcSqliteDriver(
    url = JdbcSqliteDriver.IN_MEMORY,
    properties = Properties().apply { put("foreign_keys", "true") },
  ).also { CampfireThemeDatabase.Schema.synchronous().create(it) }

  private val db = ThemingDatabaseFactory(driver).create()

  private val overrides = RoleOverrides(
    light = mapOf(ThemeRole.Primary to Color(0xFF8A2BE2), ThemeRole.Surface to Color(0xFFFFF8E1)),
    dark = mapOf(ThemeRole.OnSurface to Color(0xFFFFE0B2)),
  )

  @Test
  fun aSavedTheme_keepsItsFineTunedColors() = runTest {
    repository().saveCustomTheme(customTheme(overrides))

    val loaded = repository().getCustomTheme(THEME_ID).getOrThrow()

    assertThat(loaded).isInstanceOf<AppTheme.Fixed.Custom>()
      .prop(AppTheme.Fixed.Custom::roleOverrides)
      .isEqualTo(overrides)
  }

  @Test
  fun savingAgain_replacesTheFineTunedColors() = runTest {
    val repository = repository()
    repository.saveCustomTheme(customTheme(overrides))

    val edited = RoleOverrides(dark = mapOf(ThemeRole.Outline to Color(0xFF9E9E9E)))
    repository.saveCustomTheme(customTheme(edited))

    assertThat(repository().getCustomTheme(THEME_ID).getOrThrow())
      .isInstanceOf<AppTheme.Fixed.Custom>()
      .prop(AppTheme.Fixed.Custom::roleOverrides)
      .isEqualTo(edited)
  }

  @Test
  fun customThemes_carryTheirOwnFineTunedColors() = runTest {
    val repository = repository()
    repository.saveCustomTheme(customTheme(overrides))
    repository.saveCustomTheme(customTheme(RoleOverrides.None, id = "plain"))

    val themes = repository.observeCustomThemes().first()
      .filterIsInstance<AppTheme.Fixed.Custom>()
      .associate { it.id to it.roleOverrides }

    assertThat(themes).isEqualTo(mapOf(THEME_ID to overrides, "plain" to RoleOverrides.None))
  }

  @Test
  fun theCurrentTheme_loadsWithItsFineTunedColors() = runTest {
    repository().saveCustomTheme(customTheme(overrides))
    val settings = TestThemeSettings().apply { themeId = ThemeKey.Custom(THEME_ID) }

    val repository = repository(settings)
    repository.initialize()

    assertThat(repository.observeCurrentAppTheme().value)
      .isInstanceOf<AppTheme.Fixed.Custom>()
      .prop(AppTheme.Fixed.Custom::roleOverrides)
      .isEqualTo(overrides)
  }

  @Test
  fun deletingATheme_removesItsFineTunedColors() = runTest {
    val repository = repository()
    repository.saveCustomTheme(customTheme(overrides))

    repository.deleteCustomTheme(THEME_ID)

    assertThat(db.customAppThemeRoleOverrideQueries.selectAll().awaitAsList()).isEmpty()
  }

  private fun TestScope.repository(settings: TestThemeSettings = TestThemeSettings()) = DefaultAppThemeRepository(
    themeSettings = settings,
    themingDb = db,
    dispatcherProvider = asTestDispatcherProvider(),
  )

  private fun customTheme(roleOverrides: RoleOverrides, id: String = THEME_ID) = AppTheme.Fixed.Custom(
    id = id,
    name = "Ember",
    seedColor = Color(0xFFC0532D),
    colorSpec = ColorSpec.SpecVersion.SPEC_2025,
    colorStyle = Variant.EXPRESSIVE,
    contrastLevel = 0f,
    secondaryColorOverride = null,
    tertiaryColorOverride = null,
    errorColorOverride = null,
    neutralColorOverride = null,
    neutralVariantColorOverride = null,
    icon = AppTheme.Icon.Campfire,
    colorPalette = AppTheme.Fixed.Tent.colorPalette,
    roleOverrides = roleOverrides,
  )

  private companion object {
    const val THEME_ID = "ember"
  }
}
