package app.campfire.network.envelopes

import app.campfire.network.RequestOrigin
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class EnvelopeTest {

  @Test
  fun applyToShouldInvokeApplyPostage() {
    // given
    val origin = RequestOrigin.Url("test")
    val model = TestEnvelope()

    // when
    model.applyOrigin(origin)

    // then
    assertThat(model.applyPostageInvocations).isEqualTo(1)
  }
}

class TestEnvelope : Envelope() {
  var applyPostageInvocations = 0

  override fun applyPostage() {
    applyPostageInvocations++
  }
}
