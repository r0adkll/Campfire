package app.campfire.network.models

import app.campfire.network.TestJson
import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.prop
import kotlin.test.Test

class AudioBookmarkTest {

  @Test
  fun intParsesCorrectly() {
    // given
    val audioBookmarkJson = """
      {
        "libraryItemId": "li_8gch9ve09orgn4fdz8",
        "title": "the good part",
        "time": 16,
        "createdAt": 1668120083771
      }
    """.trimIndent()

    // when
    val result = TestJson.decodeFromString<AudioBookmark>(audioBookmarkJson)

    // then
    assertThat(result).all {
      prop(AudioBookmark::libraryItemId).isEqualTo("li_8gch9ve09orgn4fdz8")
      prop(AudioBookmark::title).isEqualTo("the good part")
      prop(AudioBookmark::time).isEqualTo(16f)
      prop(AudioBookmark::createdAt).isEqualTo(1668120083771L)
    }
  }

  @Test
  fun floatParsesCorrectly() {
    // given
    val audioBookmarkJson = """
      {
        "libraryItemId": "li_8gch9ve09orgn4fdz8",
        "title": "the good part",
        "time": 4129.145,
        "createdAt": 1668120083771
      }
    """.trimIndent()

    // when
    val result = TestJson.decodeFromString<AudioBookmark>(audioBookmarkJson)

    // then
    assertThat(result).all {
      prop(AudioBookmark::libraryItemId).isEqualTo("li_8gch9ve09orgn4fdz8")
      prop(AudioBookmark::title).isEqualTo("the good part")
      prop(AudioBookmark::time).isEqualTo(4129.145f)
      prop(AudioBookmark::createdAt).isEqualTo(1668120083771L)
    }
  }
}
