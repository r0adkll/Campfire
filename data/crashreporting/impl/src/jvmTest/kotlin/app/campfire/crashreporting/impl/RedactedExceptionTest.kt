package app.campfire.crashreporting.impl

import app.campfire.core.logging.LogRedaction
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class RedactedExceptionTest {

  @BeforeTest
  fun setup() {
    LogRedaction.enabled = true
    LogRedaction.clear()
  }

  @AfterTest
  fun teardown() {
    LogRedaction.enabled = true
    LogRedaction.clear()
  }

  @Test
  fun clean_throwable_returns_the_same_instance() {
    val error = IllegalStateException("something broke", RuntimeException("root cause"))

    assertSame(error, error.redactedCopyOrSelf())
  }

  @Test
  fun url_bearing_message_is_mirrored_and_scrubbed() {
    val error = RuntimeException("Client request(GET https://abs.myserver.net:13378/api/me) invalid: 401")

    val result = error.redactedCopyOrSelf()

    assertIs<RedactedException>(result)
    assertFalse(result.message!!.contains("abs.myserver.net"))
    assertTrue(result.message!!.startsWith("java.lang.RuntimeException: "))
    assertTrue(result.message!!.contains("https://<redacted>/api/me"))
    assertContentEquals(error.stackTrace, result.stackTrace)
  }

  @Test
  fun dirty_cause_mirrors_the_whole_chain() {
    val cause = RuntimeException("UnknownHost https://abs.myserver.net")
    val error = IllegalStateException("wrapper with no url", cause)

    val result = error.redactedCopyOrSelf()

    assertIs<RedactedException>(result)
    assertEquals("java.lang.IllegalStateException: wrapper with no url", result.message)
    val mirroredCause = assertNotNull(result.cause)
    assertIs<RedactedException>(mirroredCause)
    assertFalse(mirroredCause.message!!.contains("abs.myserver.net"))
    assertContentEquals(cause.stackTrace, mirroredCause.stackTrace)
  }

  @Test
  fun registered_bare_host_marks_a_message_dirty() {
    LogRedaction.registerServerUrl("https://abs.myserver.net:13378")
    val error = RuntimeException("java.net.UnknownHostException: abs.myserver.net")

    val result = error.redactedCopyOrSelf()

    assertIs<RedactedException>(result)
    assertFalse(result.message!!.contains("abs.myserver.net"))
  }

  @Test
  fun suppressed_exceptions_are_mirrored_and_scrubbed() {
    val error = RuntimeException("clean message")
    error.addSuppressed(RuntimeException("failed to close https://abs.myserver.net/stream"))

    val result = error.redactedCopyOrSelf()

    assertIs<RedactedException>(result)
    assertEquals(1, result.suppressed.size)
    assertFalse(result.suppressed[0].message!!.contains("abs.myserver.net"))
  }

  @Test
  fun disabled_redaction_returns_the_same_instance() {
    LogRedaction.enabled = false
    val error = RuntimeException("GET https://abs.myserver.net/api/me failed")

    assertSame(error, error.redactedCopyOrSelf())
  }

  @Test
  fun cyclic_cause_chains_terminate() {
    // a's cause is still the uninitialized sentinel, so initCause legally closes the loop
    val a = RuntimeException("a https://abs.myserver.net/x")
    val b = RuntimeException("b", a)
    a.initCause(b)

    val result = a.redactedCopyOrSelf()

    assertIs<RedactedException>(result)
  }
}
