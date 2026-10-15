// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.api

import app.campfire.core.settings.EnumSetting
import app.campfire.core.settings.EnumSettingProvider
import kotlinx.coroutines.flow.StateFlow

/** How the e-book reader looks and behaves, for every book. */
interface ReaderSettings {

  var theme: ReaderTheme
  fun observeTheme(): StateFlow<ReaderTheme>

  /** The type and page layout; a book can override any of it (see [ReaderLayoutOverrides]). */
  var layout: ReaderLayout
  fun observeLayout(): StateFlow<ReaderLayout>

  /** Whether the volume keys turn pages, where the platform lets them. */
  var volumeKeysTurnPages: Boolean
  fun observeVolumeKeysTurnPages(): StateFlow<Boolean>

  var keepScreenOn: Boolean
  fun observeKeepScreenOn(): StateFlow<Boolean>
}

enum class ReaderTheme(override val storageKey: String) : EnumSetting {
  /** The app's own colors. */
  FollowApp("follow_app"),
  Light("light"),
  Sepia("sepia"),
  Dark("dark"),
  Black("black"),
  ;

  companion object : EnumSettingProvider<ReaderTheme> {
    override fun fromStorageKey(key: String?): ReaderTheme = entries.find { it.storageKey == key } ?: FollowApp
  }
}

enum class ReaderFont(override val storageKey: String) : EnumSetting {
  /** Whatever the book sets. */
  Publisher("publisher"),
  Serif("serif"),
  Sans("sans"),
  Literata("literata"),
  AtkinsonHyperlegible("atkinson_hyperlegible"),
  OpenDyslexic("open_dyslexic"),
  ;

  companion object : EnumSettingProvider<ReaderFont> {
    override fun fromStorageKey(key: String?): ReaderFont = entries.find { it.storageKey == key } ?: Publisher
  }
}

enum class ReaderMargin(override val storageKey: String) : EnumSetting {
  Narrow("narrow"),
  Normal("normal"),
  Wide("wide"),
  ;

  companion object : EnumSettingProvider<ReaderMargin> {
    override fun fromStorageKey(key: String?): ReaderMargin = entries.find { it.storageKey == key } ?: Normal
  }
}

enum class ReaderFlow(override val storageKey: String) : EnumSetting {
  /** Turn pages. */
  Paged("paged"),

  /** Scroll through each chapter. */
  Scrolled("scrolled"),
  ;

  companion object : EnumSettingProvider<ReaderFlow> {
    override fun fromStorageKey(key: String?): ReaderFlow = entries.find { it.storageKey == key } ?: Paged
  }
}

enum class ReaderPages(override val storageKey: String) : EnumSetting {
  /** Two pages side by side where the window is wide enough, one otherwise. */
  Auto("auto"),
  Single("single"),

  /** Two pages side by side, also in portrait. */
  Two("two"),
  ;

  companion object : EnumSettingProvider<ReaderPages> {
    override fun fromStorageKey(key: String?): ReaderPages = entries.find { it.storageKey == key } ?: Auto
  }
}

/** The type and page layout of a book. */
data class ReaderLayout(
  val font: ReaderFont = ReaderFont.Publisher,
  /** Text size, as a percentage of the book's own. */
  val fontScale: Int = 100,
  val lineHeight: Float = 1.5f,
  val margin: ReaderMargin = ReaderMargin.Normal,
  val justify: Boolean = true,
  val hyphenate: Boolean = true,
  val flow: ReaderFlow = ReaderFlow.Paged,
  val pages: ReaderPages = ReaderPages.Auto,
) {
  companion object {
    val FontScaleRange = 60..240
    const val FONT_SCALE_STEP = 10
    val LineHeightRange = 1.0f..2.4f
    const val LINE_HEIGHT_STEP = 0.1f
  }
}

/** Where one book's layout differs from [ReaderSettings.layout]; null follows it. */
data class ReaderLayoutOverrides(
  val font: ReaderFont? = null,
  val fontScale: Int? = null,
  val lineHeight: Float? = null,
  val margin: ReaderMargin? = null,
  val justify: Boolean? = null,
  val hyphenate: Boolean? = null,
  val flow: ReaderFlow? = null,
  val pages: ReaderPages? = null,
) {
  val isEmpty: Boolean get() = this == None

  /** [layout] with these overrides applied. */
  fun applyTo(layout: ReaderLayout) = ReaderLayout(
    font = font ?: layout.font,
    fontScale = fontScale ?: layout.fontScale,
    lineHeight = lineHeight ?: layout.lineHeight,
    margin = margin ?: layout.margin,
    justify = justify ?: layout.justify,
    hyphenate = hyphenate ?: layout.hyphenate,
    flow = flow ?: layout.flow,
    pages = pages ?: layout.pages,
  )

  companion object {
    val None = ReaderLayoutOverrides()
  }
}
