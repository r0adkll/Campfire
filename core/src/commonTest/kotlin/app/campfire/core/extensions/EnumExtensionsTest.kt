package app.campfire.core.extensions

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class EnumExtensionsTest {

  @Test
  fun enumNext() {
    val initial = TestEnum.Value1
    val next = initial.next()
    assertThat(next).isEqualTo(TestEnum.Value2)
  }

  @Test
  fun enumNextOverflow() {
    val initial = TestEnum.Value3
    val next = initial.next()
    assertThat(next).isEqualTo(TestEnum.Value1)
  }

  @Test
  fun enumPrevious() {
    val initial = TestEnum.Value3
    val next = initial.previous()
    assertThat(next).isEqualTo(TestEnum.Value2)
  }

  @Test
  fun enumPreviousOverflow() {
    val initial = TestEnum.Value1
    val next = initial.previous()
    assertThat(next).isEqualTo(TestEnum.Value3)
  }
}

enum class TestEnum {
  Value1,
  Value2,
  Value3,
}
