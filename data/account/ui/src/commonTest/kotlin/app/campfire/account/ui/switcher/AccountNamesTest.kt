// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.account.ui.switcher

import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import app.campfire.common.compose.theme.CampfireTheme
import assertk.assertThat
import assertk.assertions.isCloseTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isTrue
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class AccountNamesTest {

  @Test
  fun `a short server name keeps the switch button centered beside both names`() = runComposeUiTest {
    setAccountNames(serverName = "Home", userName = "henry")

    val root = onNodeWithTag(Tag).getBoundsInRoot()
    val button = switchButton()
    val serverText = onNodeWithText("Home").getBoundsInRoot()
    val userText = onNodeWithText("henry").getBoundsInRoot()

    assertThat(button.centerY.value).isCloseTo(root.centerY.value, Tolerance)
    assertThat(button.left).isGreaterThan(serverText.right)
    assertThat(userText.top).isGreaterThan(serverText.bottom)
  }

  @Test
  fun `a long server name takes the full width and the switch button sits on the user name line`() =
    runComposeUiTest {
      setAccountNames(serverName = LongServerName, userName = "henry")

      val root = onNodeWithTag(Tag).getBoundsInRoot()
      val button = switchButton()
      val serverText = onNodeWithText(LongServerName).getBoundsInRoot()
      val userText = onNodeWithText("henry").getBoundsInRoot()

      // The name runs past where the button starts, so it isn't squeezed beside it
      assertThat(serverText.right).isGreaterThan(button.left)
      assertThat(button.top).isGreaterThan(serverText.top)
      assertThat(button.centerY.value).isCloseTo(userText.centerY.value, Tolerance)
      // The button takes no height: the card is only as tall as both names and the 4dp gap
      assertThat(root.height.value)
        .isCloseTo((serverText.height + 4.dp + userText.height).value, Tolerance)
    }

  @Test
  fun `without a user name the server name centers with the switch button`() = runComposeUiTest {
    setAccountNames(serverName = "Loading…", userName = null)

    val root = onNodeWithTag(Tag).getBoundsInRoot()
    val button = switchButton()
    val serverText = onNodeWithText("Loading…").getBoundsInRoot()

    assertThat(serverText.centerY.value).isCloseTo(button.centerY.value, Tolerance)
    // Only the button's 48dp touch target sets the height; there's no empty user name line below
    assertThat(root.height.value).isCloseTo(48f, Tolerance)
  }

  @Test
  fun `a tap on the switch button above the user name line still switches account`() = runComposeUiTest {
    var switched = false
    setAccountNames(serverName = LongServerName, userName = "henry", onSwitchAccount = { switched = true })

    val root = onNodeWithTag(Tag).getBoundsInRoot()
    val button = switchButton()
    val userText = onNodeWithText("henry").getBoundsInRoot()
    val tapY = userText.top - 4.dp
    assertThat(tapY).isGreaterThan(button.top)

    onNodeWithTag(Tag).performTouchInput {
      click(Offset(((button.centerX - root.left).toPx()), (tapY - root.top).toPx()))
    }

    assertThat(switched).isTrue()
  }

  private fun ComposeUiTest.switchButton(): DpRect =
    onNodeWithContentDescription("Switch account").getBoundsInRoot()

  private fun ComposeUiTest.setAccountNames(
    serverName: String,
    userName: String?,
    onSwitchAccount: () -> Unit = {},
  ) {
    setContent {
      CampfireTheme {
        AccountNames(
          serverName = serverName,
          userName = userName,
          serverNameStyle = MaterialTheme.typography.headlineSmall,
          userNameStyle = MaterialTheme.typography.titleSmall,
          onSwitchAccount = onSwitchAccount,
          modifier = Modifier.width(280.dp).testTag(Tag),
        )
      }
    }
  }

  private val DpRect.centerY: Dp get() = (top + bottom) / 2
  private val DpRect.centerX: Dp get() = (left + right) / 2

  private companion object {
    const val Tag = "account-names"
    const val LongServerName = "The Grand Audiobook Archive"
    const val Tolerance = 1f
  }
}
