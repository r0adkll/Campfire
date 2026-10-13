// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.oidc

import app.campfire.network.ApiException
import app.campfire.network.AuthAudioBookShelfApi
import app.campfire.network.envelopes.AuthorizationResponse
import app.campfire.network.envelopes.LoginResponse
import app.campfire.network.models.ServerStatus
import app.campfire.network.oidc.crypto.Pkce
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.prop
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class AuthorizationUrlTest {

  private val pkce = Pkce()

  @Test
  fun requestCarriesTheServersExtraHeaders() = runTest {
    val api = FakeAuthApi(Result.success(AuthorizationResponse("https://idp.example.com/authorize?state=s")))

    api.requestAuthorizationUrl("https://abs.example.com", pkce, "s", mapOf("X-Proxy-Auth" to "secret"))

    assertThat(api.authorizationCalls).isEqualTo(
      listOf(
        AuthorizationCall(
          serverUrl = "https://abs.example.com",
          codeChallenge = pkce.codeChallenge,
          codeVerifier = pkce.codeVerifier,
          state = "s",
          extraHeaders = mapOf("X-Proxy-Auth" to "secret"),
        ),
      ),
    )
  }

  @Test
  fun returnsTheAuthorizationUrl() = runTest {
    val api = FakeAuthApi(Result.success(AuthorizationResponse("https://idp.example.com/authorize?redirect_uri=x")))

    val url = api.requestAuthorizationUrl("https://abs.example.com", pkce, "s", null).getOrThrow()

    assertThat(url.toString()).isEqualTo("https://idp.example.com/authorize?redirect_uri=x")
  }

  @Test
  fun serverErrorMessageBecomesAuthFailure() = runTest {
    val api = FakeAuthApi(Result.failure(ApiException(400, "Invalid redirect_uri")))

    val error = api.requestAuthorizationUrl("https://abs.example.com", pkce, "s", null).exceptionOrNull()

    assertThat(error).isNotNull().isInstanceOf<OpenIdException.AuthFailure>()
      .prop(Throwable::message).isEqualTo("Invalid redirect_uri")
  }

  @Test
  fun blankServerMessageFallsBackToTheCause() = runTest {
    val api = FakeAuthApi(Result.failure(ApiException(502)))

    val error = api.requestAuthorizationUrl("https://abs.example.com", pkce, "s", null).exceptionOrNull()

    assertThat(error).isNotNull().isInstanceOf<OpenIdException.AuthFailure>()
      .prop(Throwable::message).isEqualTo("API Error [502]: ")
  }

  private data class AuthorizationCall(
    val serverUrl: String,
    val codeChallenge: String,
    val codeVerifier: String,
    val state: String,
    val extraHeaders: Map<String, String>?,
  )

  private class FakeAuthApi(
    private val authorizationResult: Result<AuthorizationResponse>,
  ) : AuthAudioBookShelfApi {
    val authorizationCalls = mutableListOf<AuthorizationCall>()

    override suspend fun authorization(
      serverUrl: String,
      codeChallenge: String,
      codeVerifier: String,
      state: String,
      extraHeaders: Map<String, String>?,
    ): Result<AuthorizationResponse> {
      authorizationCalls += AuthorizationCall(serverUrl, codeChallenge, codeVerifier, state, extraHeaders)
      return authorizationResult
    }

    override suspend fun status(serverUrl: String, extraHeaders: Map<String, String>?): Result<ServerStatus> =
      error("Unused")

    override suspend fun login(
      serverUrl: String,
      username: String,
      password: String,
      extraHeaders: Map<String, String>?,
    ): Result<LoginResponse> = error("Unused")

    override suspend fun refresh(
      serverUrl: String,
      refreshToken: String,
      extraHeaders: Map<String, String>?,
    ): Result<LoginResponse> = error("Unused")

    override suspend fun oauth(
      serverUrl: String,
      state: String,
      code: String,
      codeVerifier: String,
      extraHeaders: Map<String, String>?,
    ): Result<LoginResponse> = error("Unused")
  }
}
