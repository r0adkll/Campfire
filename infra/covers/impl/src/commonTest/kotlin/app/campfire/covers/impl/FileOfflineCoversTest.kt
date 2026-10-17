// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.covers.impl

import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray

class FileOfflineCoversTest {

  private val root = tempCoverDirectory()

  private fun covers() = FileOfflineCovers(storage = { root.toString() }, dispatcherProvider = testDispatchers)

  @AfterTest
  fun cleanUp() = root.deleteRecursively()

  @Test
  fun savedCover_isKept_acrossARestart() = runTest {
    covers().save("li_1", timestamp = 5, bytes = COVER)

    val kept = covers().get("li_1")

    assertThat(kept).isNotNull()
    assertThat(kept!!.timestamp).isEqualTo(5L)
    assertThat(Path(kept.path).readBytes()).isEqualTo(COVER)
  }

  @Test
  fun savingANewVersion_replacesTheOldOne() = runTest {
    val covers = covers()
    covers.save("li_1", timestamp = 5, bytes = COVER)
    val old = covers.get("li_1")!!.path

    covers.save("li_1", timestamp = 6, bytes = byteArrayOf(9))

    assertThat(SystemFileSystem.exists(Path(old))).isFalse()
    assertThat(covers().get("li_1")!!.timestamp).isEqualTo(6L)
    assertThat(SystemFileSystem.list(root).map { it.name }).containsExactlyInAnyOrder("li_1.6")
  }

  @Test
  fun coverWithoutATimestamp_isKeptUnversioned() = runTest {
    covers().save("li_1", timestamp = null, bytes = COVER)

    assertThat(covers().get("li_1")!!.timestamp).isNull()
  }

  @Test
  fun deletedCover_isGone() = runTest {
    val covers = covers()
    covers.save("li_1", timestamp = 5, bytes = COVER)
    covers.save("li_2", timestamp = 5, bytes = COVER)

    covers.delete("li_1")

    assertThat(covers.get("li_1")).isNull()
    assertThat(covers().itemIds()).containsExactlyInAnyOrder("li_2")
  }

  @Test
  fun leftovers_fromAnInterruptedWrite_areCleared() = runTest {
    SystemFileSystem.createDirectories(root)
    SystemFileSystem.sink(Path(root, "li_1.5.part")).buffered().use { it.write(COVER) }
    SystemFileSystem.sink(Path(root, "li_2.4")).buffered().use { it.write(COVER) }
    SystemFileSystem.sink(Path(root, "li_2.7")).buffered().use { it.write(COVER) }

    val covers = covers()

    assertThat(covers.itemIds()).containsExactlyInAnyOrder("li_2")
    assertThat(covers.get("li_2")!!.timestamp).isEqualTo(7L)
    assertThat(SystemFileSystem.list(root).map { it.name }).containsExactlyInAnyOrder("li_2.7")
  }

  private fun Path.readBytes() = SystemFileSystem.source(this).buffered().use { it.readByteArray() }

  private companion object {
    val COVER = byteArrayOf(1, 2, 3)
  }
}
