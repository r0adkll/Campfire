// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.ui.connect

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.campfire.common.compose.theme.CampfireTheme

/*
 * Previews of every state the setup link confirmation can be in.
 */

private const val PhoneWidth = 411
private const val PhoneHeight = 891

private val NewServer = ConnectContent.Confirm(
  serverHost = "abs.example.com",
  serverName = "Example Audiobooks",
  existingUserName = null,
  signInWithOpenId = true,
)

@Preview(name = "New server, OpenID", widthDp = PhoneWidth, heightDp = PhoneHeight)
@Composable
private fun ConnectNewOpenIdPreview() = ConnectPreview(NewServer)

@Preview(name = "New server, OpenID, dark", widthDp = PhoneWidth, heightDp = PhoneHeight)
@Composable
private fun ConnectNewOpenIdDarkPreview() = ConnectPreview(NewServer, useDarkColors = true)

@Preview(name = "New server, password", widthDp = PhoneWidth, heightDp = PhoneHeight)
@Composable
private fun ConnectNewPasswordPreview() = ConnectPreview(NewServer.copy(signInWithOpenId = false))

@Preview(name = "New server, no name", widthDp = PhoneWidth, heightDp = PhoneHeight)
@Composable
private fun ConnectNewUnnamedPreview() = ConnectPreview(NewServer.copy(serverName = null))

@Preview(name = "Saving", widthDp = PhoneWidth, heightDp = PhoneHeight)
@Composable
private fun ConnectSavingPreview() = ConnectPreview(NewServer.copy(isSaving = true))

@Preview(name = "Update an account's token", widthDp = PhoneWidth, heightDp = PhoneHeight)
@Composable
private fun ConnectUpdatePreview() = ConnectPreview(NewServer.copy(existingUserName = "alice"))

@Preview(name = "Token updated", widthDp = PhoneWidth, heightDp = PhoneHeight)
@Composable
private fun ConnectUpdatedPreview() = ConnectPreview(
  ConnectContent.Updated(serverHost = "abs.example.com", userName = "alice"),
)

@Preview(name = "Needs a newer Campfire", widthDp = PhoneWidth, heightDp = PhoneHeight)
@Composable
private fun ConnectUnsupportedPreview() = ConnectPreview(ConnectContent.UnsupportedVersion)

@Preview(name = "Broken link", widthDp = PhoneWidth, heightDp = PhoneHeight)
@Composable
private fun ConnectInvalidPreview() = ConnectPreview(ConnectContent.Invalid)

@Preview(name = "Broken link, dark", widthDp = PhoneWidth, heightDp = PhoneHeight)
@Composable
private fun ConnectInvalidDarkPreview() = ConnectPreview(ConnectContent.Invalid, useDarkColors = true)

@Preview(name = "Loading", widthDp = PhoneWidth, heightDp = PhoneHeight)
@Composable
private fun ConnectLoadingPreview() = ConnectPreview(ConnectContent.Loading)

@Composable
private fun ConnectPreview(
  content: ConnectContent,
  useDarkColors: Boolean = false,
) {
  CampfireTheme(useDarkColors = useDarkColors) {
    Connect(state = ConnectUiState(content = content, eventSink = {}), modifier = Modifier)
  }
}
