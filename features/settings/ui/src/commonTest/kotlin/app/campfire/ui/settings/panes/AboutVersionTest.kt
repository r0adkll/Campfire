// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.settings.panes

import app.campfire.core.app.ApplicationInfo
import app.campfire.core.app.BuildCommit
import app.campfire.core.app.Flavor
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone

class AboutVersionTest {

  private val november = Instant.parse("2026-11-24T14:03:27Z")
  private val minusFive = UtcOffset(hours = -5).asTimeZone()

  private fun info(buildCommit: BuildCommit? = BuildCommit(november, "220c0be")) = ApplicationInfo(
    packageName = "app.campfire",
    debugBuild = false,
    flavor = Flavor.Standard,
    versionName = "1.4.0",
    versionCode = 1040099,
    osName = "Test",
    osVersion = "1",
    buildCommit = buildCommit,
  )

  @Test
  fun `commit line shows local time with its offset and the sha`() {
    assertThat(BuildCommit(november, "220c0be").line(minusFive)).isEqualTo("2026-11-24 09:03 -05:00 · 220c0be")
  }

  @Test
  fun `a zero offset is written as +00 00 rather than Z`() {
    assertThat(BuildCommit(november, "220c0be").line(TimeZone.UTC)).isEqualTo("2026-11-24 14:03 +00:00 · 220c0be")
  }

  @Test
  fun `the offset follows daylight saving time`() {
    val newYork = TimeZone.of("America/New_York")
    val july = Instant.parse("2026-07-01T14:03:00Z")

    assertThat(BuildCommit(july, sha = null).line(newYork)).isEqualTo("2026-07-01 10:03 -04:00")
    assertThat(BuildCommit(november, sha = null).line(newYork)).isEqualTo("2026-11-24 09:03 -05:00")
  }

  @Test
  fun `half-hour offsets keep their minutes and sign`() {
    val india = UtcOffset(hours = 5, minutes = 30).asTimeZone()
    val newfoundland = UtcOffset(hours = -3, minutes = -30).asTimeZone()

    assertThat(BuildCommit(november, sha = null).line(india)).isEqualTo("2026-11-24 19:33 +05:30")
    assertThat(BuildCommit(november, sha = null).line(newfoundland)).isEqualTo("2026-11-24 10:33 -03:30")
  }

  @Test
  fun `commit line drops whichever half is unknown`() {
    assertThat(BuildCommit(november, sha = null).line(minusFive)).isEqualTo("2026-11-24 09:03 -05:00")
    assertThat(BuildCommit(time = null, sha = "220c0be").line(minusFive)).isEqualTo("220c0be")
  }

  @Test
  fun `copy text joins version and commit line`() {
    assertThat(info().versionCopyText(minusFive))
      .isEqualTo("1.4.0 (1040099) · 2026-11-24 09:03 -05:00 · 220c0be")
  }

  @Test
  fun `copy text is just the version without git info`() {
    assertThat(info(buildCommit = null).versionCopyText(minusFive)).isEqualTo("1.4.0 (1040099)")
  }
}
