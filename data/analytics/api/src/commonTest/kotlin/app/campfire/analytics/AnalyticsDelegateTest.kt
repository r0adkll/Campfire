package app.campfire.analytics

import app.campfire.analytics.events.ScreenView
import assertk.assertThat
import assertk.assertions.containsExactly
import kotlin.test.Test

class AnalyticsDelegateTest {

  @Test
  fun `adding delegate receives events`() {
    // given
    val delegate = FakeAnalytics()
    val event = ScreenView("TestScreen")
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
    val event1 = ScreenView("TestScreen1")
    val event2 = ScreenView("TestScreen2")
    Analytics.Delegator += delegate

    // when
    Analytics.send(event1)
    Analytics.Delegator -= delegate
    Analytics.send(event2)

    // then
    assertThat(delegate.events).containsExactly(event1)
  }
}
