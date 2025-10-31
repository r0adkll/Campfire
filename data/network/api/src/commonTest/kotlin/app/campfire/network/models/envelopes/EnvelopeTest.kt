package app.campfire.network.models.envelopes

import app.campfire.network.RequestOrigin
import app.campfire.network.envelopes.Envelope
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class EnvelopeTest {

  @Test
  fun `applyTo will invoke an Envelopes applyPostage() method`() {
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
