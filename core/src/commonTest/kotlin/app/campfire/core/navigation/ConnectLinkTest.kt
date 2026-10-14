// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.navigation

import app.campfire.core.navigation.ConnectLinkResult.Invalid
import app.campfire.core.navigation.ConnectLinkResult.Reason
import app.campfire.core.navigation.ConnectLinkResult.UnsupportedVersion
import app.campfire.core.navigation.ConnectLinkResult.Valid
import assertk.assertThat
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.test.Test

class ConnectLinkTest {

  @Test
  fun parses_a_link_as_url_search_params_writes_it() {
    // new URLSearchParams({v: "1", server, name, auth: "openid", id, secret}).toString()
    val result = ConnectLink.parse(
      "v=1&server=https%3A%2F%2Fabs.firenation.app&name=Fire+Nation&auth=openid" +
        "&id=$CLIENT_ID&secret=$CLIENT_SECRET",
    )

    assertThat(result).isEqualTo(
      Valid(
        ConnectLink(
          serverUrl = "https://abs.firenation.app",
          serverName = "Fire Nation",
          clientId = CLIENT_ID,
          clientSecret = CLIENT_SECRET,
          signInWithOpenId = true,
        ),
      ),
    )
  }

  @Test
  fun name_and_auth_are_optional() {
    val link = parseValid("v=1&server=https://abs.example.com&id=$CLIENT_ID&secret=$CLIENT_SECRET")

    assertThat(link.serverName).isNull()
    assertThat(link.signInWithOpenId).isFalse()
  }

  @Test
  fun only_openid_auth_starts_openid_sign_in() {
    assertThat(parseValid(validFragment(auth = "OpenID")).signInWithOpenId).isTrue()
    assertThat(parseValid(validFragment(auth = "password")).signInWithOpenId).isFalse()
  }

  @Test
  fun a_leading_hash_is_allowed() {
    assertThat(ConnectLink.parse("#" + validFragment())).isInstanceOf<Valid>()
  }

  @Test
  fun values_are_trimmed() {
    val link = parseValid(
      "v=+1+&server=+https%3A%2F%2Fabs.example.com%2F+&name=%20Home%20" +
        "&id=+$CLIENT_ID%0A&secret=%09$CLIENT_SECRET+",
    )

    assertThat(link.serverUrl).isEqualTo("https://abs.example.com")
    assertThat(link.serverName).isEqualTo("Home")
    assertThat(link.clientId).isEqualTo(CLIENT_ID)
    assertThat(link.clientSecret).isEqualTo(CLIENT_SECRET)
  }

  @Test
  fun unknown_keys_are_ignored() {
    assertThat(ConnectLink.parse(validFragment() + "&theme=forest&utm_source=discord&flag"))
      .isInstanceOf<Valid>()
  }

  @Test
  fun percent_escapes_decode_as_utf8() {
    assertThat(parseValid(validFragment(name = "Caf%C3%A9+%26+Books")).serverName).isEqualTo("Café & Books")
  }

  @Test
  fun control_characters_are_dropped_from_the_name_and_it_is_capped() {
    assertThat(parseValid(validFragment(name = "Fire%0ANation%07")).serverName).isEqualTo("FireNation")
    assertThat(parseValid(validFragment(name = "a".repeat(100))).serverName).isEqualTo("a".repeat(64))
    assertThat(parseValid(validFragment(name = "+%0A+")).serverName).isNull()
  }

  @Test
  fun the_server_url_is_normalized() {
    assertThat(parseValid(validFragment(server = "HTTPS://ABS.Example.COM/")).serverUrl)
      .isEqualTo("https://abs.example.com")
    assertThat(parseValid(validFragment(server = "https://example.com:8443/audiobookshelf/")).serverUrl)
      .isEqualTo("https://example.com:8443/audiobookshelf")
    assertThat(parseValid(validFragment(server = "https://[2001:db8::1]:13378")).serverUrl)
      .isEqualTo("https://[2001:db8::1]:13378")
  }

  @Test
  fun the_server_host_is_what_the_token_is_sent_to() {
    assertThat(parseValid(validFragment(server = "https://abs.example.com:8443/abs")).serverHost)
      .isEqualTo("abs.example.com:8443")
  }

  @Test
  fun a_newer_version_asks_for_an_update() {
    assertThat(ConnectLink.parse(validFragment(version = "2"))).isEqualTo(UnsupportedVersion)
    assertThat(ConnectLink.parse(validFragment(version = "1.1"))).isEqualTo(UnsupportedVersion)
    // Even when the rest of it doesn't look like a version 1 link
    assertThat(ConnectLink.parse("v=2&endpoint=https://abs.example.com")).isEqualTo(UnsupportedVersion)
  }

  @Test
  fun a_link_without_a_version_is_malformed() {
    assertThat(ConnectLink.parse("server=https://abs.example.com&id=$CLIENT_ID&secret=$CLIENT_SECRET"))
      .isEqualTo(Invalid(Reason.Malformed))
    assertThat(ConnectLink.parse("")).isEqualTo(Invalid(Reason.Malformed))
  }

  @Test
  fun bad_escapes_are_malformed() {
    assertThat(ConnectLink.parse(validFragment(name = "100%"))).isEqualTo(Invalid(Reason.Malformed))
    assertThat(ConnectLink.parse(validFragment(name = "%zz"))).isEqualTo(Invalid(Reason.Malformed))
    assertThat(ConnectLink.parse(validFragment(name = "%C3"))).isEqualTo(Invalid(Reason.Malformed))
  }

  @Test
  fun a_repeated_field_is_malformed() {
    assertThat(ConnectLink.parse(validFragment() + "&server=https://evil.example"))
      .isEqualTo(Invalid(Reason.Malformed))
  }

  @Test
  fun the_server_must_be_an_https_url_with_a_host() {
    listOf(
      null,
      "",
      "abs.example.com",
      "http://abs.example.com",
      "ftp://abs.example.com",
      "https://",
      "https:///path",
      "https://abs.example.com@evil.example",
      "https://user:pass@abs.example.com",
      "https://abs.example.com?x=1",
      "https://abs.example.com/path?x=1",
      "https://abs.example.com#frag",
      "https://abs example.com",
      "https://abs.example.com/a b",
      "https://-abs.example.com",
      "https://abs..example.com",
      "https://abs.example.com:",
      "https://abs.example.com:0",
      "https://abs.example.com:70000",
      "https://abs.example.com:443x",
      "https://ábs.example.com",
      "https://abs.example.com\\@evil.example",
      "https://[::1",
    ).forEach { server ->
      assertThat(ConnectLink.parse(validFragment(server = server)), name = "server=$server")
        .isEqualTo(Invalid(Reason.InvalidServer))
    }
  }

  @Test
  fun the_service_token_is_required() {
    listOf(
      validFragment(id = null),
      validFragment(secret = null),
      validFragment(id = "+"),
      validFragment(secret = ""),
      validFragment(secret = "abc%0D%0AX-Injected:+1"),
      validFragment(id = "two+words"),
    ).forEach { fragment ->
      assertThat(ConnectLink.parse(fragment), name = fragment)
        .isEqualTo(Invalid(Reason.InvalidServiceToken))
    }
  }

  @Test
  fun the_token_is_kept_out_of_to_string() {
    val text = parseValid(validFragment()).toString()

    assertThat(text).doesNotContain(CLIENT_SECRET)
    assertThat(text).doesNotContain(CLIENT_ID)
    assertThat(ConnectLink.parse(validFragment()).toString()).doesNotContain(CLIENT_SECRET)
  }

  private fun parseValid(fragment: String): ConnectLink {
    val result = ConnectLink.parse(fragment)
    assertThat(result, name = fragment).isInstanceOf<Valid>()
    return (result as Valid).link
  }

  private fun validFragment(
    version: String? = "1",
    server: String? = "https%3A%2F%2Fabs.example.com",
    name: String? = "Home",
    auth: String? = null,
    id: String? = CLIENT_ID,
    secret: String? = CLIENT_SECRET,
  ): String = listOf("v" to version, "server" to server, "name" to name, "auth" to auth, "id" to id, "secret" to secret)
    .filter { it.second != null }
    .joinToString("&") { (key, value) -> "$key=$value" }

  private companion object {
    const val CLIENT_ID = "0123456789abcdef0123456789abcdef.access"
    const val CLIENT_SECRET = "0123456789abcdef0123456789abcdef0123456789abcdef012345"
  }
}
