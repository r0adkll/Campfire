// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth

import app.campfire.account.api.AbsToken
import app.campfire.account.api.AccountManager
import app.campfire.auth.api.AuthException
import app.campfire.auth.local.UserStorageStrategy
import app.campfire.core.model.Server
import app.campfire.core.model.User as DomainUser
import app.campfire.core.model.UserId
import app.campfire.network.ApiException
import app.campfire.network.AuthAudioBookShelfApi
import app.campfire.network.envelopes.AuthorizationResponse
import app.campfire.network.envelopes.LoginResponse
import app.campfire.network.models.ServerSettings
import app.campfire.network.models.ServerStatus
import app.campfire.network.models.User as NetworkUser
import app.campfire.network.models.UserPermissions
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.json.Json

class DefaultAuthRepositoryTest {

  private val api = FakeAuthApi()
  private val accountManager = FakeAccountManager()
  private val newUserStorage = FakeUserStorageStrategy()
  private val existingUserStorage = FakeUserStorageStrategy()

  private val repository = DefaultAuthRepository(
    api = api,
    accountManager = accountManager,
    newUserStorageStrategy = newUserStorage,
    existingUserStorageStrategy = existingUserStorage,
  )

  @Test
  fun `login with no accessible libraries fails with NoAccessibleLibraries`() = runTest {
    api.loginResult = Result.success(loginResponse(userDefaultLibraryId = null))

    val result = repository.authenticate(SERVER_URL, SERVER_NAME, "user", "pass")

    assertThat(result.exceptionOrNull()).isNotNull().isInstanceOf<AuthException.NoAccessibleLibraries>()
    assertThat(newUserStorage.stored).isFalse()
    assertThat(accountManager.addedAccount).isFalse()
  }

  @Test
  fun `oauth login with no accessible libraries fails with NoAccessibleLibraries`() = runTest {
    api.oauthResult = Result.success(loginResponse(userDefaultLibraryId = null))

    val result = repository.authenticate(SERVER_URL, SERVER_NAME, "verifier", "code", "state")

    assertThat(result.exceptionOrNull()).isNotNull().isInstanceOf<AuthException.NoAccessibleLibraries>()
    assertThat(newUserStorage.stored).isFalse()
    assertThat(accountManager.addedAccount).isFalse()
  }

  @Test
  fun `login with a default library succeeds and stores the user`() = runTest {
    api.loginResult = Result.success(loginResponse(userDefaultLibraryId = "lib_1"))

    val result = repository.authenticate(SERVER_URL, SERVER_NAME, "user", "pass")

    assertThat(result.isSuccess).isTrue()
    assertThat(newUserStorage.stored).isTrue()
    assertThat(newUserStorage.storedDefaultLibraryId).isEqualTo("lib_1")
    assertThat(accountManager.addedAccount).isTrue()
  }

  @Test
  fun `login rejected with 401 fails with InvalidCredentials`() = runTest {
    api.loginResult = Result.failure(ApiException(401, "Invalid user or password"))

    val result = repository.authenticate(SERVER_URL, SERVER_NAME, "user", "wrong-pass")

    assertThat(result.exceptionOrNull()).isNotNull().isInstanceOf<AuthException.InvalidCredentials>()
  }

  @Test
  fun `login network failure fails with Network`() = runTest {
    api.loginResult = Result.failure(IOException("connection refused"))

    val result = repository.authenticate(SERVER_URL, SERVER_NAME, "user", "pass")

    assertThat(result.exceptionOrNull()).isNotNull().isInstanceOf<AuthException.Network>()
  }

  @Test
  fun `login serialization failure fails with UnexpectedResponse`() = runTest {
    api.loginResult = Result.failure(IllegalStateException("Unexpected JSON token"))

    val result = repository.authenticate(SERVER_URL, SERVER_NAME, "user", "pass")

    assertThat(result.exceptionOrNull()).isNotNull().isInstanceOf<AuthException.UnexpectedResponse>()
  }

  @Test
  fun `login response without tokens fails with UnexpectedResponse`() = runTest {
    api.loginResult = Result.success(loginResponse(userDefaultLibraryId = "lib_1", accessToken = null))

    val result = repository.authenticate(SERVER_URL, SERVER_NAME, "user", "pass")

    assertThat(result.exceptionOrNull()).isNotNull().isInstanceOf<AuthException.UnexpectedResponse>()
    assertThat(newUserStorage.stored).isFalse()
  }

  @Test
  fun `login response with null userDefaultLibraryId deserializes`() {
    val json = Json { ignoreUnknownKeys = true }
    val response = json.decodeFromString<LoginResponse>(NO_LIBRARY_LOGIN_JSON)

    assertThat(response.userDefaultLibraryId).isNull()
  }

  private fun loginResponse(
    userDefaultLibraryId: String?,
    accessToken: String? = "access-token",
  ): LoginResponse = LoginResponse(
    user = NetworkUser(
      id = "usr_1",
      username = "testuser",
      type = "user",
      accessToken = accessToken,
      refreshToken = "refresh-token",
      mediaProgress = emptyList(),
      seriesHideFromContinueListening = emptyList(),
      bookmarks = emptyList(),
      isActive = true,
      isLocked = false,
      lastSeen = null,
      createdAt = 0L,
      permissions = UserPermissions(
        download = true,
        update = false,
        delete = false,
        upload = false,
        accessAllLibraries = true,
        accessAllTags = true,
        accessExplicitContent = true,
      ),
      librariesAccessible = emptyList(),
    ),
    userDefaultLibraryId = userDefaultLibraryId,
    serverSettings = serverSettings(),
    source = "docker",
  )

  private fun serverSettings(): ServerSettings = ServerSettings(
    id = "server-settings",
    scannerFindCovers = false,
    scannerCoverProvider = "google",
    scannerParseSubtitle = false,
    scannerPreferMatchedMetadata = false,
    scannerDisableWatcher = false,
    storeCoverWithItem = false,
    storeMetadataWithItem = false,
    metadataFileFormat = "json",
    rateLimitLoginRequests = 10,
    rateLimitLoginWindow = 600000L,
    backupSchedule = "30 1 * * *",
    backupsToKeep = 2,
    maxBackupSize = 1,
    loggerDailyLogsToKeep = 7,
    loggerScannerLogsToKeep = 2,
    homeBookshelfView = 1,
    bookshelfView = 1,
    sortingIgnorePrefix = false,
    sortingPrefixes = listOf("the"),
    chromecastEnabled = false,
    dateFormat = "MM/dd/yyyy",
    timeFormat = "HH:mm",
    language = "en-us",
    logLevel = 2,
    version = "2.36.0",
  )

  private class FakeAuthApi : AuthAudioBookShelfApi {
    var loginResult: Result<LoginResponse> = Result.failure(IllegalStateException("not stubbed"))
    var oauthResult: Result<LoginResponse> = Result.failure(IllegalStateException("not stubbed"))

    override suspend fun status(
      serverUrl: String,
      extraHeaders: Map<String, String>?,
    ): Result<ServerStatus> = Result.failure(IllegalStateException("not stubbed"))

    override suspend fun login(
      serverUrl: String,
      username: String,
      password: String,
      extraHeaders: Map<String, String>?,
    ): Result<LoginResponse> = loginResult

    override suspend fun authorization(
      serverUrl: String,
      codeChallenge: String,
      codeVerifier: String,
      state: String,
      extraHeaders: Map<String, String>?,
    ): Result<AuthorizationResponse> = Result.failure(IllegalStateException("not stubbed"))

    override suspend fun oauth(
      serverUrl: String,
      state: String,
      code: String,
      codeVerifier: String,
      extraHeaders: Map<String, String>?,
    ): Result<LoginResponse> = oauthResult
  }

  private class FakeAccountManager : AccountManager {
    var addedAccount: Boolean = false

    override suspend fun addAccount(
      serverUrl: String,
      accessToken: String,
      refreshToken: String?,
      extraHeaders: Map<String, String>?,
      user: DomainUser,
    ) {
      addedAccount = true
    }

    override suspend fun invalidateAccount(user: DomainUser) = Unit
    override suspend fun switchAccount(user: DomainUser) = Unit
    override suspend fun logout(server: Server) = Unit
    override suspend fun getToken(userId: UserId): AbsToken? = null
    override suspend fun updateToken(userId: UserId, newToken: AbsToken) = Unit
    override suspend fun getExtraHeaders(userId: UserId): Map<String, String>? = null
  }

  private class FakeUserStorageStrategy : UserStorageStrategy {
    var stored: Boolean = false
    var storedDefaultLibraryId: String? = null

    override suspend fun store(
      serverName: String,
      serverUrl: String,
      serverSettings: ServerSettings,
      user: NetworkUser,
      userDefaultLibraryId: String,
    ) {
      stored = true
      storedDefaultLibraryId = userDefaultLibraryId
    }
  }

  companion object {
    private const val SERVER_URL = "https://abs.example.com"
    private const val SERVER_NAME = "Test Campsite"

    // Trimmed-down /login response from an ABS server where the user has no library access
    private val NO_LIBRARY_LOGIN_JSON = """
      {
        "user": {
          "id": "usr_1",
          "username": "testuser",
          "type": "user",
          "accessToken": "access-token",
          "mediaProgress": [],
          "seriesHideFromContinueListening": [],
          "bookmarks": [],
          "isActive": true,
          "isLocked": false,
          "lastSeen": null,
          "createdAt": 1633522963509,
          "permissions": {
            "download": true,
            "update": false,
            "delete": false,
            "upload": false,
            "accessAllLibraries": true,
            "accessAllTags": true,
            "accessExplicitContent": true
          },
          "librariesAccessible": []
        },
        "userDefaultLibraryId": null,
        "serverSettings": {
          "id": "server-settings",
          "scannerFindCovers": false,
          "scannerCoverProvider": "google",
          "scannerParseSubtitle": false,
          "scannerPreferMatchedMetadata": false,
          "scannerDisableWatcher": false,
          "storeCoverWithItem": false,
          "storeMetadataWithItem": false,
          "metadataFileFormat": "json",
          "rateLimitLoginRequests": 10,
          "rateLimitLoginWindow": 600000,
          "backupSchedule": "30 1 * * *",
          "backupsToKeep": 2,
          "maxBackupSize": 1,
          "loggerDailyLogsToKeep": 7,
          "loggerScannerLogsToKeep": 2,
          "homeBookshelfView": 1,
          "bookshelfView": 1,
          "sortingIgnorePrefix": false,
          "sortingPrefixes": ["the"],
          "chromecastEnabled": false,
          "dateFormat": "MM/dd/yyyy",
          "timeFormat": "HH:mm",
          "language": "en-us",
          "logLevel": 2,
          "version": "2.36.0"
        },
        "Source": "docker"
      }
    """.trimIndent()
  }
}
