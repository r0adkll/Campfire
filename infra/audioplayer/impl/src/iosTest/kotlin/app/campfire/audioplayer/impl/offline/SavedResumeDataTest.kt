// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.offline

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.cinterop.BetaInteropApi
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUUID
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding

@OptIn(BetaInteropApi::class)
class SavedResumeDataTest {

  private val directory = Path(NSTemporaryDirectory(), "resume-${NSUUID().UUIDString}")
    .also { SystemFileSystem.createDirectories(it) }
  private val part = Path(directory, "1.m4b.part")
  private val data = NSString.create(string = "resume data").dataUsingEncoding(NSUTF8StringEncoding)!!

  @AfterTest
  fun tearDown() {
    directory.deleteRecursively()
  }

  @Test
  fun savedResumeDataIsReadBackOnce() {
    SavedResumeData.save(part, offset = 1234, data = data)

    val taken = assertNotNull(SavedResumeData.take(part))
    assertEquals(1234L, taken.offset)
    assertEquals(data, taken.data)
    assertNull(SavedResumeData.take(part))
  }

  @Test
  fun nothingIsSavedOnceTheDownloadDirectoryIsGone() {
    directory.deleteRecursively()

    SavedResumeData.save(part, offset = 0, data = data)

    assertFalse(SavedResumeData.file(part).isFile())
  }

  @Test
  fun anUnreadableFileIsDiscarded() {
    SystemFileSystem.sink(SavedResumeData.file(part)).close()

    assertNull(SavedResumeData.take(part))
    assertFalse(SavedResumeData.file(part).isFile())
  }
}
