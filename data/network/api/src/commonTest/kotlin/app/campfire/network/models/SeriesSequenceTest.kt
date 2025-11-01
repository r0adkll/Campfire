package app.campfire.network.models

import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.isNullOrEmpty
import assertk.assertions.prop
import kotlin.test.Test
import kotlinx.serialization.json.Json

class SeriesSequenceTest {

  // This must match the JSON configuration in
  // HttpClientModule.kt
  private val json = Json {
    isLenient = true
    ignoreUnknownKeys = true
  }

  @Test
  fun successfulSeriesSequenceDeserialization() {
    // given
    val sequenceJson = """
      {
        "id": "test_id",
        "name": "test_name",
        "sequence": "10"
      }
    """.trimIndent()

    // when
    val result = json.decodeFromString<SeriesSequence>(sequenceJson)
    assertThat(result).all {
      prop(SeriesSequence::id).isEqualTo("test_id")
      prop(SeriesSequence::name).isEqualTo("test_name")
      prop(SeriesSequence::sequence).isEqualTo("10")
    }
  }

  @Test
  fun successfulSeriesSequenceDecodingWithEmptySequence() {
    // given
    val sequenceJson = """
      {
        "id": "test_id",
        "name": "test_name",
        "sequence": ""
      }
    """.trimIndent()

    // when
    val result = json.decodeFromString<SeriesSequence>(sequenceJson)
    assertThat(result).all {
      prop(SeriesSequence::id).isEqualTo("test_id")
      prop(SeriesSequence::name).isEqualTo("test_name")
      prop(SeriesSequence::sequence).isNullOrEmpty()
    }
  }

  @Test
  fun successfulSeriesSequenceDecodingWithNullSequence() {
    // given
    val sequenceJson = """
      {
        "id": "test_id",
        "name": "test_name",
        "sequence": null
      }
    """.trimIndent()

    // when
    val result = json.decodeFromString<SeriesSequence>(sequenceJson)
    assertThat(result).all {
      prop(SeriesSequence::id).isEqualTo("test_id")
      prop(SeriesSequence::name).isEqualTo("test_name")
      prop(SeriesSequence::sequence).isNull()
    }
  }

  @Test
  fun successfulSeriesSequenceDecodingWithMissingSequence() {
    // given
    val sequenceJson = """
      {
        "id": "test_id",
        "name": "test_name"
      }
    """.trimIndent()

    // when
    val result = json.decodeFromString<SeriesSequence>(sequenceJson)
    assertThat(result).all {
      prop(SeriesSequence::id).isEqualTo("test_id")
      prop(SeriesSequence::name).isEqualTo("test_name")
      prop(SeriesSequence::sequence).isNull()
    }
  }
}
