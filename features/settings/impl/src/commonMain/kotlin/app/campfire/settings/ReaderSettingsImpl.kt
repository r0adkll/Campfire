// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import app.campfire.core.di.AppScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.settings.api.ReaderFlow
import app.campfire.settings.api.ReaderFont
import app.campfire.settings.api.ReaderLayout
import app.campfire.settings.api.ReaderMargin
import app.campfire.settings.api.ReaderPages
import app.campfire.settings.api.ReaderSettings
import app.campfire.settings.api.ReaderTheme
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalSettingsApi::class)
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<ReaderSettings>())
@Inject
class ReaderSettingsImpl(
  @ForScope(AppScope::class) override val settings: ObservableSettings,
  @ForScope(AppScope::class) override val scope: CoroutineScope,
) : ReaderSettings, AppSettings() {

  private val themeProperty = enumSetting(KEY_READER_THEME, ReaderTheme)
  override var theme: ReaderTheme by themeProperty
  override fun observeTheme(): StateFlow<ReaderTheme> = themeProperty.observe()

  private val fontProperty = enumSetting(KEY_READER_FONT, ReaderFont)
  private val fontScaleProperty = longSetting(KEY_READER_FONT_SCALE, ReaderDefaults.fontScale.toLong())
  private val lineHeightProperty = floatSetting(KEY_READER_LINE_HEIGHT, ReaderDefaults.lineHeight)
  private val marginProperty = enumSetting(KEY_READER_MARGIN, ReaderMargin)
  private val justifyProperty = booleanSetting(KEY_READER_JUSTIFY, ReaderDefaults.justify)
  private val hyphenateProperty = booleanSetting(KEY_READER_HYPHENATE, ReaderDefaults.hyphenate)
  private val flowProperty = enumSetting(KEY_READER_FLOW, ReaderFlow)
  private val pagesProperty = enumSetting(KEY_READER_PAGES, ReaderPages)

  private var font by fontProperty
  private var fontScale by fontScaleProperty
  private var lineHeight by lineHeightProperty
  private var margin by marginProperty
  private var justify by justifyProperty
  private var hyphenate by hyphenateProperty
  private var flow by flowProperty
  private var pages by pagesProperty

  override var layout: ReaderLayout
    get() = ReaderLayout(font, fontScale.toInt(), lineHeight, margin, justify, hyphenate, flow, pages)
    set(value) {
      font = value.font
      fontScale = value.fontScale.toLong()
      lineHeight = value.lineHeight
      margin = value.margin
      justify = value.justify
      hyphenate = value.hyphenate
      flow = value.flow
      pages = value.pages
    }

  private val layoutFlow: StateFlow<ReaderLayout> by lazy {
    combine<Any, ReaderLayout>(
      listOf(
        fontProperty.observe(),
        fontScaleProperty.observe(),
        lineHeightProperty.observe(),
        marginProperty.observe(),
        justifyProperty.observe(),
        hyphenateProperty.observe(),
        flowProperty.observe(),
        pagesProperty.observe(),
      ),
    ) { values ->
      ReaderLayout(
        font = values[0] as ReaderFont,
        fontScale = (values[1] as Long).toInt(),
        lineHeight = values[2] as Float,
        margin = values[3] as ReaderMargin,
        justify = values[4] as Boolean,
        hyphenate = values[5] as Boolean,
        flow = values[6] as ReaderFlow,
        pages = values[7] as ReaderPages,
      )
    }.stateIn(scope, SharingStarted.Eagerly, layout)
  }

  override fun observeLayout(): StateFlow<ReaderLayout> = layoutFlow

  private val volumeKeysProperty = booleanSetting(KEY_READER_VOLUME_KEYS, true)
  override var volumeKeysTurnPages: Boolean by volumeKeysProperty
  override fun observeVolumeKeysTurnPages(): StateFlow<Boolean> = volumeKeysProperty.observe()

  private val keepScreenOnProperty = booleanSetting(KEY_READER_KEEP_SCREEN_ON, true)
  override var keepScreenOn: Boolean by keepScreenOnProperty
  override fun observeKeepScreenOn(): StateFlow<Boolean> = keepScreenOnProperty.observe()
}

private val ReaderDefaults = ReaderLayout()

private const val KEY_READER_THEME = "reader_theme"
private const val KEY_READER_FONT = "reader_font"
private const val KEY_READER_FONT_SCALE = "reader_font_scale"
private const val KEY_READER_LINE_HEIGHT = "reader_line_height"
private const val KEY_READER_MARGIN = "reader_margin"
private const val KEY_READER_JUSTIFY = "reader_justify"
private const val KEY_READER_HYPHENATE = "reader_hyphenate"
private const val KEY_READER_FLOW = "reader_flow"
private const val KEY_READER_PAGES = "reader_pages"
private const val KEY_READER_VOLUME_KEYS = "reader_volume_keys"
private const val KEY_READER_KEEP_SCREEN_ON = "reader_keep_screen_on"
