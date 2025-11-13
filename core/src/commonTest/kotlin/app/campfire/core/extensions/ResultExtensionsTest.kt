package app.campfire.core.extensions

import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isInstanceOf
import assertk.assertions.isSuccess
import assertk.assertions.prop
import kotlin.test.Test

class ResultExtensionsTest {

  @Test
  fun resultWithOther_BothSuccess() {
    val result1 = Result.success(1)
    val result2 = Result.success(2)

    val together = result1.with(result2) { a, b -> a + b }

    assertThat(together)
      .isSuccess()
      .isEqualTo(3)
  }

  @Test
  fun resultWithOther_FirstFailure() {
    val result1 = Result.failure<Int>(Exception("result1_error"))
    val result2 = Result.success(2)

    val together = result1.with(result2) { a, b -> a + b }

    assertThat(together)
      .isFailure()
      .isInstanceOf<FusedResultFailure>()
      .all {
        prop(FusedResultFailure::first).isFailure()
        prop(FusedResultFailure::second).isSuccess()
      }
  }

  @Test
  fun resultWithOther_SecondFailure() {
    val result1 = Result.success(1)
    val result2 = Result.failure<Int>(Exception("result2_error"))

    val together = result1.with(result2) { a, b -> a + b }

    assertThat(together)
      .isFailure()
      .isInstanceOf<FusedResultFailure>()
      .all {
        prop(FusedResultFailure::first).isSuccess()
        prop(FusedResultFailure::second).isFailure()
      }
  }
}
