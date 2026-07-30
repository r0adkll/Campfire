// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.di.qualifier.ForScope
import app.campfire.settings.api.AndroidAutoCategory
import app.campfire.settings.api.AndroidAutoCategoryConfig
import app.campfire.settings.api.AndroidAutoSettings
import com.r0adkll.kimchi.annotations.ContributesBinding
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import me.tatarka.inject.annotations.Inject

@OptIn(ExperimentalSettingsApi::class)
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, boundType = AndroidAutoSettings::class)
@Inject
class AndroidAutoSettingsImpl(
  override val settings: ObservableSettings,
  @ForScope(AppScope::class) override val scope: CoroutineScope,
) : AndroidAutoSettings, AppSettings() {

  private val orderProperty = customSetting(
    key = PREF_AA_ORDER,
    defaultValue = AndroidAutoCategory.entries.toList(),
    getter = { raw -> raw.decodeOrder() },
    setter = { order -> order.encodeOrder() },
  )

  private val hiddenProperty = customSetting(
    key = PREF_AA_HIDDEN,
    defaultValue = emptySet<AndroidAutoCategory>(),
    getter = { raw -> raw.decodeCategorySet() },
    setter = { hidden -> hidden.encodeCategorySet() },
  )

  private val gridOverridesProperty = customSetting(
    key = PREF_AA_GRID_OVERRIDES,
    defaultValue = emptyMap<AndroidAutoCategory, Boolean>(),
    getter = { raw -> raw.decodeGridOverrides() },
    setter = { overrides -> overrides.encodeGridOverrides() },
  )

  private var storedOrder: List<AndroidAutoCategory> by orderProperty
  private var hiddenCategories: Set<AndroidAutoCategory> by hiddenProperty
  private var gridOverrides: Map<AndroidAutoCategory, Boolean> by gridOverridesProperty

  private val categoryConfigsFlow: StateFlow<List<AndroidAutoCategoryConfig>> = combine(
    orderProperty.observe(),
    hiddenProperty.observe(),
    gridOverridesProperty.observe(),
  ) { order, hidden, overrides ->
    buildConfigs(order, hidden, overrides)
  }.stateIn(
    scope = scope,
    started = SharingStarted.Lazily,
    initialValue = buildConfigs(storedOrder, hiddenCategories, gridOverrides),
  )

  override fun observeCategoryConfigs(): StateFlow<List<AndroidAutoCategoryConfig>> = categoryConfigsFlow

  override fun setCategoryVisible(category: AndroidAutoCategory, visible: Boolean) {
    if (category.alwaysVisible && !visible) return
    hiddenCategories = if (visible) {
      hiddenCategories - category
    } else {
      hiddenCategories + category
    }
  }

  override fun setCategoryGridLayout(category: AndroidAutoCategory, isGrid: Boolean) {
    gridOverrides = if (isGrid == category.defaultGridLayout) {
      gridOverrides - category
    } else {
      gridOverrides + (category to isGrid)
    }
  }

  override fun setCategoryOrder(order: List<AndroidAutoCategory>) {
    storedOrder = normalizeOrder(order)
  }

  private fun buildConfigs(
    order: List<AndroidAutoCategory>,
    hidden: Set<AndroidAutoCategory>,
    overrides: Map<AndroidAutoCategory, Boolean>,
  ): List<AndroidAutoCategoryConfig> {
    return normalizeOrder(order).map { category ->
      AndroidAutoCategoryConfig(
        category = category,
        visible = category.alwaysVisible || category !in hidden,
        isGridLayout = overrides[category] ?: category.defaultGridLayout,
      )
    }
  }

  /**
   * Reconciles a desired order with the pinned constraint: pinned categories always sit at
   * their declaration position, regardless of what the stored / incoming order says.
   */
  private fun normalizeOrder(order: List<AndroidAutoCategory>): List<AndroidAutoCategory> {
    val deduped = order.distinct()
    val withMissing = deduped + AndroidAutoCategory.entries.filterNot { it in deduped }
    val nonPinned = withMissing.filterNot { it.pinned }.toMutableList()
    return buildList {
      AndroidAutoCategory.entries.forEach { category ->
        if (category.pinned) add(category)
      }
      addAll(nonPinned)
    }
  }
}

internal const val PREF_AA_ORDER = "pref_android_auto_category_order"
internal const val PREF_AA_HIDDEN = "pref_android_auto_hidden_categories"
internal const val PREF_AA_GRID_OVERRIDES = "pref_android_auto_grid_overrides"

private const val LIST_SEPARATOR = ":"
private const val ENTRY_SEPARATOR = "="

private fun String.decodeOrder(): List<AndroidAutoCategory> =
  split(LIST_SEPARATOR).mapNotNull(AndroidAutoCategory::fromStorageKey)

private fun List<AndroidAutoCategory>.encodeOrder(): String =
  joinToString(LIST_SEPARATOR) { it.storageKey }

private fun String.decodeCategorySet(): Set<AndroidAutoCategory> =
  if (isEmpty()) emptySet() else split(LIST_SEPARATOR).mapNotNull(AndroidAutoCategory::fromStorageKey).toSet()

private fun Set<AndroidAutoCategory>.encodeCategorySet(): String =
  joinToString(LIST_SEPARATOR) { it.storageKey }

private fun String.decodeGridOverrides(): Map<AndroidAutoCategory, Boolean> {
  if (isEmpty()) return emptyMap()
  return split(LIST_SEPARATOR).mapNotNull { entry ->
    val (key, value) = entry.split(ENTRY_SEPARATOR, limit = 2).takeIf { it.size == 2 }
      ?: return@mapNotNull null
    val category = AndroidAutoCategory.fromStorageKey(key) ?: return@mapNotNull null
    category to value.toBooleanStrictOrNull()
  }.mapNotNull { (category, bool) -> bool?.let { category to it } }.toMap()
}

private fun Map<AndroidAutoCategory, Boolean>.encodeGridOverrides(): String =
  entries.joinToString(LIST_SEPARATOR) { (category, isGrid) ->
    "${category.storageKey}$ENTRY_SEPARATOR$isGrid"
  }
