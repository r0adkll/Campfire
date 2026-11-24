// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.settings.panes

import app.campfire.core.app.ApplicationInfo
import app.campfire.core.app.Flavor
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone

class AboutVersionTest {

  private val commitTime = Instant.parse("2026-11-24T14:03:27Z")
  private val newYork = UtcOffset(hours = -5).asTimeZone()

  private fun info(commitTime: Instant? = this.commitTime, commitSha: String? = "220c0be") = ApplicationInfo(
    packageName = "app.campfire",
    debugBuild = false,
    flavor = Flavor.Standard,
    versionName = "1.4.0",
    versionCode = 1040099,
    osName = "Test",
    osVersion = "1",
    commitTime = commitTime,
    commitSha = commitSha,
  )

  @Test
  fun `commit line shows local time with its offset and the sha`() {
    assertThat(info().commitLine(newYork)).isEqualTo("2026-11-24 09:03 -05:00 · 220c0be")
  }

  @Test
  fun `a zero offset is written as +00 00 rather than Z`() {
    assertThat(info().commitLine(TimeZone.UTC)).isEqualTo("2026-11-24 14:03 +00:00 · 220c0be")
  }

  @Test
  fun `commit line drops whichever half is unknown`() {
    assertThat(info(commitSha = null).commitLine(newYork)).isEqualTo("2026-11-24 09:03 -05:00")
    assertThat(info(commitTime = null).commitLine(newYork)).isEqualTo("220c0be")
  }

  @Test
  fun `commit line is absent without git info`() {
    assertThat(info(commitTime = null, commitSha = null).commitLine(newYork)).isNull()
  }

  @Test
  fun `copy text joins version and commit line`() {
    assertThat(info().versionCopyText(newYork))
      .isEqualTo("1.4.0 (1040099) · 2026-11-24 09:03 -05:00 · 220c0be")
  }

  @Test
  fun `copy text is just the version without git info`() {
    assertThat(info(commitTime = null, commitSha = null).versionCopyText(newYork))
      .isEqualTo("1.4.0 (1040099)")
  }
}
