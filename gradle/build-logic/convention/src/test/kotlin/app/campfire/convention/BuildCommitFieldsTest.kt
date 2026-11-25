// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.convention

import kotlin.test.Test
import kotlin.test.assertEquals

class BuildCommitFieldsTest {

  @Test
  fun `plain text is quoted as is`() {
    assertEquals("\"Show the build commit (#1494)\"", "Show the build commit (#1494)".toSourceLiteral())
  }

  @Test
  fun `quotes and backslashes are escaped`() {
    assertEquals("""  "Say \"hi\" to C:\\dir"  """.trim(), """Say "hi" to C:\dir""".toSourceLiteral())
  }

  @Test
  fun `a dollar can't start a Kotlin template`() {
    assertEquals("\"Fix \\u0024{name} and \\u0024x\"", "Fix \${name} and \$x".toSourceLiteral())
  }

  @Test
  fun `a backslash before u stays a backslash in Java`() {
    // `\\u0041` is not a Java unicode escape, since its backslash is itself escaped.
    assertEquals("\"\\\\u0041\"", "\\u0041".toSourceLiteral())
  }

  @Test
  fun `control characters become spaces`() {
    assertEquals("\"a b\"", "a\tb".toSourceLiteral())
  }

  @Test
  fun `reads time, short sha and the message's first line`() {
    val log = "1764000000\u0000$FULL_SHA\u0000Show the build commit\nwrapped onto a second line\n\nBody text."

    assertEquals(BuildCommitFields(1_764_000_000L, "220c0be", "Show the build commit"), parseBuildCommit(log))
  }

  @Test
  fun `an empty message is no subject`() {
    val log = "1764000000\u0000$FULL_SHA\u0000"

    assertEquals(BuildCommitFields(1_764_000_000L, "220c0be", ""), parseBuildCommit(log))
  }

  @Test
  fun `no git is no build commit`() {
    assertEquals(BuildCommitFields(0L, "", ""), parseBuildCommit(null))
    assertEquals(BuildCommitFields(0L, "", ""), parseBuildCommit("garbage"))
  }

  @Test
  fun `declares every field`() {
    val fields = mutableListOf<Triple<String, String, String>>()
    BuildCommitFields(1_764_000_000L, "220c0be", "Ship it").declare { type, name, value ->
      fields += Triple(type, name, value)
    }

    assertEquals(
      listOf(
        Triple("long", "COMMIT_TIME", "1764000000L"),
        Triple("String", "COMMIT_SHA", "\"220c0be\""),
        Triple("String", "COMMIT_SUBJECT", "\"Ship it\""),
      ),
      fields,
    )
  }

  private companion object {
    const val FULL_SHA = "220c0be1234567890abcdef1234567890abcdef1"
  }
}
