// Copyright 2023, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: Apache-2.0

package app.campfire.convention.changelog

import kotlin.test.Test
import kotlin.test.assertEquals

class ChangelogParserTest {

  private val sample = """
    # Change Log

    All notable changes to this project will be documented in this file.

    ## [Unreleased]

    ### Added

    ### Fixed

    - A fixed "thing" with quotes

    ## [1.2.0] - 2026-07-01

    ### Added

    - New feature one
    - New feature two

    ### Changed

    - Some change

    ## [1.1.0] - 2026-06-01

    ### Fixed

    - Old fix

    [1.2.0]: https://github.com/example/releases/1.2.0
    [1.1.0]: https://github.com/example/releases/1.1.0
  """.trimIndent().lines()

  @Test
  fun `parses versions with change sets`() {
    val versions = ChangelogParser.parse(sample)

    val v120 = versions.first { it.version == "1.2.0" }
    assertEquals("2026-07-01", v120.date)
    assertEquals(listOf("Added", "Changed"), v120.changeSets.map { it.name })
    assertEquals(listOf("New feature one", "New feature two"), v120.changeSets[0].changes)
  }

  // Quirk pinned for CLI parity: a version header without a date yields "" — not null —
  // because the optional regex group resolves to an empty string.
  @Test
  fun `unreleased section has empty-string date`() {
    val versions = ChangelogParser.parse(sample)
    assertEquals("", versions.first { it.version == "Unreleased" }.date)
  }

  // Quirk pinned for CLI parity: the final version section is only flushed by the NEXT
  // `## ` header. Trailing link-reference lines don't flush it, so the oldest release
  // (1.1.0 here) is dropped. If this quirk is ever fixed, fix scripts/app's
  // ChangelogParser in the same release.
  @Test
  fun `last version section is dropped when not followed by another header`() {
    val versions = ChangelogParser.parse(sample)
    assertEquals(listOf("Unreleased", "1.2.0"), versions.map { it.version })
  }

  @Test
  fun `json output is compact with explicit nulls and escaped strings`() {
    val versions = listOf(
      ChangelogVersion(
        version = "1.0.0",
        date = null,
        changeSets = listOf(
          ChangelogVersion.ChangeSet(name = null, changes = listOf("""He said "hi"""")),
        ),
      ),
    )

    assertEquals(
      """[{"version":"1.0.0","date":null,"changes":[{"name":null,"changes":["He said \"hi\""]}]}]""",
      ChangelogParser.toJson(versions),
    )
  }

  @Test
  fun `empty version list encodes as empty array`() {
    assertEquals("[]", ChangelogParser.toJson(emptyList()))
  }
}
