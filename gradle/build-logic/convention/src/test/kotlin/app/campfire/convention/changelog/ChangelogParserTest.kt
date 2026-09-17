// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.convention.changelog

import app.campfire.convention.changelog.ChangelogPlatform.ANDROID
import app.campfire.convention.changelog.ChangelogPlatform.DESKTOP
import app.campfire.convention.changelog.ChangelogPlatform.IOS
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChangelogParserTest {

  private val sample = """
    # Change Log

    All notable changes to this project will be documented in this file.

    ## [Unreleased]

    ### Added

    - [Desktop] Desktop only
    - [android, iOS] Mobile only
    - Everywhere

    ### Fixed

    - A fixed "thing" with quotes

    ## [1.2.0]

    ### Added

    - Shared in the first desktop release

    ## [1.1.0] - 2026-07-01

    ### Added

    - New feature one
    - New feature two

    ### Changed

    - Some change

    ## [0.1.0-alpha] - 2026-06-01

    ### Fixed

    - Old fix

    [1.1.0]: https://github.com/example/releases/1.1.0
    [0.1.0-alpha]: https://github.com/example/releases/0.1.0-alpha
  """.trimIndent().lines()

  private val versions = ChangelogParser.parse(sample)

  private fun version(name: String) = versions.first { it.version == name }

  @Test
  fun `parses every version with its change sets`() {
    assertEquals(listOf("Unreleased", "1.2.0", "1.1.0", "0.1.0-alpha"), versions.map { it.version })

    val v110 = version("1.1.0")
    assertEquals("2026-07-01", v110.date)
    assertEquals(listOf("Added", "Changed"), v110.changeSets.map { it.name })
    assertEquals(listOf("New feature one", "New feature two"), v110.changeSets[0].changes.map { it.text })
  }

  @Test
  fun `version without a date has a null date`() {
    assertNull(version("Unreleased").date)
  }

  @Test
  fun `platform tags are stripped and resolved case-insensitively`() {
    val added = version("Unreleased").changeSets.first().changes

    assertEquals(ChangelogVersion.Change("Desktop only", setOf(DESKTOP)), added[0])
    assertEquals(ChangelogVersion.Change("Mobile only", setOf(ANDROID, IOS)), added[1])
  }

  @Test
  fun `untagged unreleased entries apply to every platform`() {
    val everywhere = version("Unreleased").changeSets.first().changes[2]
    assertEquals(setOf(ANDROID, DESKTOP, IOS), everywhere.platforms)
  }

  @Test
  fun `untagged released entries apply only to platforms that had shipped`() {
    assertEquals(setOf(ANDROID, DESKTOP), version("1.2.0").changeSets.single().changes.single().platforms)
    assertEquals(setOf(ANDROID), version("1.1.0").changeSets.first().changes.first().platforms)
    assertEquals(setOf(ANDROID), version("0.1.0-alpha").changeSets.single().changes.single().platforms)
  }

  @Test
  fun `platform whose first release is not in the changelog yet only gets unreleased entries`() {
    val versions = ChangelogParser.parse(
      """
      ## [Unreleased]

      ### Added

      - New

      ## [0.1.0-alpha]

      ### Added

      - Old
      """.trimIndent().lines(),
    )

    assertTrue(DESKTOP in versions[0].changeSets.single().changes.single().platforms)
    assertEquals(setOf(ANDROID), versions[1].changeSets.single().changes.single().platforms)
  }

  @Test
  fun `unknown platform tag fails the parse`() {
    val error = assertFailsWith<IllegalStateException> {
      ChangelogParser.parse(listOf("## [Unreleased]", "### Added", "- [Destkop] Typo"))
    }
    assertTrue("Destkop" in error.message.orEmpty())
  }

  @Test
  fun `json output is compact with explicit nulls, escaped strings and ordered platforms`() {
    val versions = listOf(
      ChangelogVersion(
        version = "1.0.0",
        date = null,
        changeSets = listOf(
          ChangelogVersion.ChangeSet(
            name = null,
            changes = listOf(ChangelogVersion.Change("""He said "hi"""", setOf(IOS, ANDROID))),
          ),
        ),
      ),
    )

    assertEquals(
      """[{"version":"1.0.0","date":null,"changes":[{"name":null,"changes":""" +
        """[{"text":"He said \"hi\"","platforms":["ANDROID","IOS"]}]}]}]""",
      ChangelogParser.toJson(versions),
    )
  }

  @Test
  fun `empty version list encodes as empty array`() {
    assertEquals("[]", ChangelogParser.toJson(emptyList()))
  }
}
