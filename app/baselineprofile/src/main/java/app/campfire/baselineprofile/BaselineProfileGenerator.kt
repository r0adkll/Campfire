package app.campfire.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiAutomatorTestScope
import androidx.test.uiautomator.onElement
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

        // Find and click an item out of the home feed to open the detail page
        onElement { isScrollable }
          .onElement { isClickable }
          .click()
      }
    }
  }
}

fun UiAutomatorTestScope.handleSignIn() {
  val welcomeScreenElement = onElementOrNull { textAsString() == "Add a campsite" }
  if (welcomeScreenElement != null) {
    welcomeScreenElement.click()
    waitForStableInActiveWindow()
    onElement { textAsString() == "Add campsite" }.click()
    waitForStableInActiveWindow()
    onElement { contentDescription == "Apply analytics consent" }.click()
  }
}
