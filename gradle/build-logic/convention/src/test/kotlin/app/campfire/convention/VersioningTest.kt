// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.convention

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VersioningTest {

  @Test
  fun `derives codes that preserve release ordering`() {
    val rc3 = deriveVersionCode("1.0.0-rc3")!!
    val final = deriveVersionCode("1.0.0")!!
    val patchRc = deriveVersionCode("1.0.1-rc1")!!

    assertEquals(1_000_003, rc3)
    assertEquals(1_000_099, final)
    assertEquals(1_000_101, patchRc)
    assertTrue(rc3 < final && final < patchRc)
  }

  @Test
  fun `accepts v prefix and case-insensitive rc`() {
    assertEquals(1_020_304, deriveVersionCode("v1.2.3-RC4"))
  }

  @Test
  fun `rejects non-semver and alpha tags`() {
    assertNull(deriveVersionCode("0.2.1-alpha"))
    assertNull(deriveVersionCode("not-a-version"))
    assertNull(deriveVersionCode("1.0"))
  }
}
