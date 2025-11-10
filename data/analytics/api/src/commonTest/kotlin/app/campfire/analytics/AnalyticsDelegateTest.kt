package app.campfire.analytics

import app.campfire.analytics.events.ScreenViewEvent
import app.campfire.analytics.test.FakeAnalytics
import assertk.assertThat
import assertk.assertions.containsExactly
import kotlin.test.Test

class AnalyticsDelegateTest {

  @Test
  fun `adding delegate receives events`() {
    // given
    val delegate = FakeAnalytics()
    val event = ScreenViewEvent("TestScreen")
    Analytics.Delegator += delegate

    // when
    Analytics.send(event)

    // then
    assertThat(delegate.events).containsExactly(event)
  }

  @Test
  fun `removing delegate stops receiving events`() {
    // given
    val delegate = FakeAnalytics()
    val event1 = ScreenViewEvent("TestScreen1")
    val event2 = ScreenViewEvent("TestScreen2")
    Analytics.Delegator += delegate

    // when
    Analytics.send(event1)
    Analytics.Delegator -= delegate
    Analytics.send(event2)

    // then
    assertThat(delegate.events).containsExactly(event1)
  }
}
