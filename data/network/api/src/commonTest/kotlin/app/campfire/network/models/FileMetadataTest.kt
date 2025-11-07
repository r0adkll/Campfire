package app.campfire.network.models

import app.campfire.network.TestJson
import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.prop
import kotlin.test.Test

class FileMetadataTest {

  @Test
  fun smallIntParsesCorrectly() {
    // given
    val fileMetadataJson = """
      {
        "filename": "test file name",
        "ext": "test ext",
        "path": "test path",
        "relPath": "test rel path",
        "size": 5,
        "mtimeMs": 9223372036854775807,
        "ctimeMs": 9223372036854775807,
        "birthtimeMs": 9223372036854775807
      }
    """.trimIndent()

    // when
    val result = TestJson.decodeFromString<FileMetadata>(fileMetadataJson)

    // then
    assertThat(result).all {
      prop(FileMetadata::filename).isEqualTo("test file name")
      prop(FileMetadata::ext).isEqualTo("test ext")
      prop(FileMetadata::path).isEqualTo("test path")
      prop(FileMetadata::relPath).isEqualTo("test rel path")
      prop(FileMetadata::size).isEqualTo(5)
      prop(FileMetadata::mtimeMs).isEqualTo(Long.MAX_VALUE)
      prop(FileMetadata::ctimeMs).isEqualTo(Long.MAX_VALUE)
      prop(FileMetadata::birthtimeMs).isEqualTo(Long.MAX_VALUE)
    }
  }

  @Test
  fun largestLongParsesCorrectly() {
    // given
    val fileMetadataJson = """
      {
        "filename": "test file name",
        "ext": "test ext",
        "path": "test path",
        "relPath": "test rel path",
        "size": 9223372036854775807,
        "mtimeMs": 9223372036854775807,
        "ctimeMs": 9223372036854775807,
        "birthtimeMs": 9223372036854775807
      }
    """.trimIndent()

    // when
    val result = TestJson.decodeFromString<FileMetadata>(fileMetadataJson)

    // then
    assertThat(result).all {
      prop(FileMetadata::filename).isEqualTo("test file name")
      prop(FileMetadata::ext).isEqualTo("test ext")
      prop(FileMetadata::path).isEqualTo("test path")
      prop(FileMetadata::relPath).isEqualTo("test rel path")
      prop(FileMetadata::size).isEqualTo(Long.MAX_VALUE)
      prop(FileMetadata::mtimeMs).isEqualTo(Long.MAX_VALUE)
      prop(FileMetadata::ctimeMs).isEqualTo(Long.MAX_VALUE)
      prop(FileMetadata::birthtimeMs).isEqualTo(Long.MAX_VALUE)
    }
  }
}
