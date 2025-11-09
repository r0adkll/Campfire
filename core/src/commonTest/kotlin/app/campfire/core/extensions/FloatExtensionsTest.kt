package app.campfire.core.extensions

import app.cash.burst.Burst
import app.cash.burst.burstValues
import assertk.assertThat
import assertk.assertions.isNotNull
import kotlin.test.Test

@Burst
class FloatExtensionsTest {

  @Test
  fun toStringDecimalPlaces(
    floatValue: Float = burstValues(0f, 10f, 23.3f, 485.1234f, -128.9876f),
    decimalPlaces: Int = burstValues(0, 1, 2, 3),
  ) {
    val floatString = floatValue.toString(decimalPlaces)
    println("Float[$floatValue] ==> [$floatString]")
    assertThat(floatString.toFloatOrNull()).isNotNull()
  }
}
