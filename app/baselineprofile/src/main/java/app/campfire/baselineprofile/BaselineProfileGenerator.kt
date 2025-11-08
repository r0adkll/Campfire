package app.campfire.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiAutomatorTestScope
import androidx.test.uiautomator.onElement
import androidx.test.uiautomator.onElementOrNull
import androidx.test.uiautomator.textAsString
import androidx.test.uiautomator.uiAutomator
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This test class generates a baseline startup baseline profile for the target package as well
 * as startup profiles for dex layout optimization.
 **/
@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

  @get:Rule
  val rule = BaselineProfileRule()

  @Test
  fun startup() {
    rule.collect(
      packageName = InstrumentationRegistry.getArguments().getString("targetAppId")
        ?: throw Exception("targetAppId not passed as instrumentation runner arg"),
      includeInStartupProfile = true,
    ) {
      uiAutomator {
        startApp(packageName = packageName)
        handleSignIn()
      }
    }
  }

  @Test
  fun itemDetail() {
    rule.collect(
      packageName = InstrumentationRegistry.getArguments().getString("targetAppId")
        ?: throw Exception("targetAppId not passed as instrumentation runner arg"),
    ) {
      uiAutomator {
        startApp(packageName = packageName)

        // Log the user in, if the are not already
        handleSignIn()

        // Let's make sure the app is stable
        waitForStableInActiveWindow()

        // IF the home feed loads, click on an item
        onElementOrNull { isScrollable }
          ?.onElementOrNull { isClickable }
          ?.click()
      }
    }
  }
}

fun UiAutomatorTestScope.handleSignIn() {
  val welcomeScreenElement = onElementOrNull { textAsString() == "Add a campsite" }
  if (welcomeScreenElement != null) {
    // Click through to the Login Screen
    welcomeScreenElement.click()
    waitForStableInActiveWindow()

    // Click to Login (Credentials pre-supplied via Gradle)
    onElement { textAsString() == "Add campsite" }.click()
    waitForStableInActiveWindow()

    // Click through the Analytics Consent Page
    onElementOrNull { textAsString() == "Continue" }?.click()
  }
}
