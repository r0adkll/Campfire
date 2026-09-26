// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.socket.impl

import app.campfire.account.api.AccountManager
import app.campfire.account.api.TokenRefresher
import app.campfire.account.api.UserSessionManager
import app.campfire.core.di.AppScope
import app.campfire.core.di.Scoped
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.lifecycle.AppLifecycleObserver
import app.campfire.core.logging.Corked
import app.campfire.core.session.UserSession
import app.campfire.network.RequestOrigin
import app.campfire.network.reachability.ServerReachability
import app.campfire.settings.api.CampfireSettings
import app.campfire.settings.api.DevSettings
import app.campfire.socket.SocketManager
import app.campfire.socket.SocketState
import app.campfire.socket.events.AuthorAdded
import app.campfire.socket.events.AuthorRemoved
import app.campfire.socket.events.AuthorUpdated
import app.campfire.socket.events.CollectionAdded
import app.campfire.socket.events.CollectionRemoved
import app.campfire.socket.events.CollectionUpdated
import app.campfire.socket.events.EpisodeAdded
import app.campfire.socket.events.EpisodeDownloadFinished
import app.campfire.socket.events.EpisodeDownloadQueueCleared
import app.campfire.socket.events.EpisodeDownloadQueued
import app.campfire.socket.events.EpisodeDownloadStarted
import app.campfire.socket.events.ItemAdded
import app.campfire.socket.events.ItemRemoved
import app.campfire.socket.events.ItemUpdated
import app.campfire.socket.events.ItemsAdded
import app.campfire.socket.events.ItemsUpdated
import app.campfire.socket.events.LibraryAdded
import app.campfire.socket.events.LibraryRemoved
import app.campfire.socket.events.LibraryUpdated
import app.campfire.socket.events.NotificationsUpdated
import app.campfire.socket.events.PlaylistAdded
import app.campfire.socket.events.PlaylistRemoved
import app.campfire.socket.events.PlaylistUpdated
import app.campfire.socket.events.SeriesAdded
import app.campfire.socket.events.SeriesRemoved
import app.campfire.socket.events.SeriesUpdated
import app.campfire.socket.events.SocketEvent
import app.campfire.socket.events.UserItemProgressUpdated
import app.campfire.socket.events.UserSessionClosed
import app.campfire.socket.events.UserUpdated
import app.campfire.socket.impl.logging.NoOpLogging
import com.piasy.kmp.socketio.engineio.transports.WebSocket
import com.piasy.kmp.socketio.socketio.IO
import com.piasy.kmp.socketio.socketio.Manager
import com.piasy.kmp.socketio.socketio.Socket
import com.piasy.kmp.xlog.Logging
import com.r0adkll.kimchi.annotations.ContributesBinding
import com.r0adkll.kimchi.annotations.ContributesMultibinding
import dev.jordond.connectivity.Connectivity
import kotlin.concurrent.Volatile
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import me.tatarka.inject.annotations.Inject

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class DefaultSocketManager(
  private val userSessionManager: UserSessionManager,
  private val accountManager: AccountManager,
  private val tokenRefresher: TokenRefresher,
  private val appLifecycleObserver: AppLifecycleObserver,
  private val settings: CampfireSettings,
  private val connectivity: Connectivity,
  private val serverReachability: ServerReachability,
  private val devSettings: DevSettings,
  @ForScope(AppScope::class) private val coroutineScope: CoroutineScope,
) : SocketManager {

  companion object : Corked("DefaultSocketManager") {
    private const val MAX_AUTH_RETRIES = 3
    private const val RECONNECTION_DELAY_MS = 2_000L
    private const val RECONNECTION_DELAY_MAX_MS = 60_000L

    // While the server is unreachable the socket is the idle-time check (HTTP only retries when
    // something makes a request), so its attempts follow the same growing gaps: 1m up to 10m
    private const val UNREACHABLE_DELAY_MS = 60_000L
    private const val UNREACHABLE_DELAY_MAX_MS = 600_000L

    init {
      // Configure kmp-socketio's xlog backend exactly once per process. The default backend
      // routes through platform-native loggers we don't want — `NoOpLogging` silences it and
      // lets our own bark logs carry the load. Runs at class-load time so it's idempotent
      // across recreations of the (App-scoped) singleton.
      Logging.init(NoOpLogging())
    }
  }

  @Volatile
  private var authFailureCount: Int = 0

  /** The access token most recently sent in an `auth` emit, so retries can tell the
   * refresher exactly which token the server rejected. */
  @Volatile
  private var lastAuthToken: String? = null

  private val _state = MutableStateFlow<SocketState>(SocketState.Disconnected)
  override val state: StateFlow<SocketState> = _state.asStateFlow()

  private val _events = MutableSharedFlow<SocketEvent>(extraBufferCapacity = 64)
  override val events: SharedFlow<SocketEvent> = _events.asSharedFlow()

  private val json = Json {
    ignoreUnknownKeys = true
  }

  /**
   * A managed list of events we can handle and parse from socket.io
   */
  private val eventConfigs = listOf(
    UserItemProgressUpdated,
    UserSessionClosed,
    UserUpdated,
    ItemAdded,
    ItemUpdated,
    ItemRemoved,
    ItemsAdded,
    ItemsUpdated,
    LibraryAdded,
    LibraryUpdated,
    LibraryRemoved,
    SeriesAdded,
    SeriesUpdated,
    SeriesRemoved,
    CollectionAdded,
    CollectionUpdated,
    CollectionRemoved,
    AuthorAdded,
    AuthorUpdated,
    AuthorRemoved,
    PlaylistAdded,
    PlaylistUpdated,
    PlaylistRemoved,
    EpisodeAdded,
    EpisodeDownloadQueued,
    EpisodeDownloadStarted,
    EpisodeDownloadFinished,
    EpisodeDownloadQueueCleared,
    NotificationsUpdated,
  )

  @Volatile
  private var socket: Socket? = null

  /** Connection-demand and reachability observers for the currently active socket; cancelled on
   * [stop] so a replaced socket's observers can't reopen the stale connection alongside the new
   * one. */
  @Volatile
  private var connectionJob: Job? = null

  /** Whether the active socket is currently demanded (foreground + network); gates reachability
   * reconnects so a server coming back never opens a socket the app doesn't want. */
  @Volatile
  private var socketDemanded: Boolean = false

  internal suspend fun start(session: UserSession.LoggedIn) {
    if (accountManager.getToken(session.user.id)?.accessToken == null) {
      wbark { "No access token for user ${session.user.id}; skipping socket connect" }
      return
    }

    val userId = session.user.id
    val url = session.user.serverUrl
    // A fresh start deserves a fresh retry budget — without this, a manual retryConnection()
    // after the retry loop gave up would hit auth_failed with zero attempts remaining.
    authFailureCount = 0
    _state.value = SocketState.Connecting

    // Custom headers (e.g. reverse-proxy auth) guard the WebSocket upgrade as much as the API
    val extraHeaders = accountManager.getExtraHeaders(userId).orEmpty()
    val opts = IO.Options().apply {
      this.transports = listOf(WebSocket.NAME)
      this.extraHeaders = extraHeaders.mapValues { (_, value) -> listOf(value) }
      // A fresh manager per start: a multiplexed one would keep the headers it was created with
      this.forceNew = true
      // socket.io defaults to retrying every <=5s forever, which keeps the radio awake while
      // the server is unreachable (off the home network, server down). Back off to a minute;
      // regaining the network or foregrounding the app resets the backoff.
      this.reconnectionDelay = RECONNECTION_DELAY_MS
      this.reconnectionDelayMax = RECONNECTION_DELAY_MAX_MS
    }
    IO.socket(url, opts) { newSocket ->
      socket = newSocket

      newSocket.on(Socket.EVENT_CONNECT) {
        ibark { "Socket connected; reading token and sending auth" }
        _state.value = SocketState.Authenticating
        coroutineScope.launch {
          val freshToken = accountManager.getToken(userId)?.accessToken
          if (freshToken != null) {
            lastAuthToken = freshToken
            newSocket.emit("auth", freshToken)
          } else {
            wbark { "No token available for socket auth" }
            _state.value = SocketState.Failed("No token")
          }
        }
      }

      newSocket.on("init") { args ->
        authFailureCount = 0
        val payload = args.firstJsonObject()
        val authedUserId = payload?.string("userId")
        val username = payload?.string("username")
        if (authedUserId != null && username != null) {
          ibark { "Socket authenticated!" }
          // The handshake proves the server is back — lift any fail-fast on HTTP requests now
          // rather than waiting for the next probe
          serverReachability.reportReachable(url)
          _state.value = SocketState.Authenticated(authedUserId, username)
        }
      }

      eventConfigs.forEach { handler ->
        newSocket.on(handler.name) { args ->
          val element = args.firstJsonElement()
          if (element != null) {
            runCatching { with(handler) { json.decode(element) } }
              .onSuccess {
                it.applyOrigin(RequestOrigin.Url(url))
                _events.tryEmit(it)
              }
              .onFailure { wbark { "Failed to parse ${handler.name}: ${it.message}" } }
          }
        }
      }

      newSocket.on("auth_failed") { args ->
        val message = args.firstJsonObject()?.string("message") ?: "Auth failed"
        val attempt = authFailureCount + 1
        wbark { "Socket auth failed: $message (attempt $attempt/$MAX_AUTH_RETRIES)" }
        _state.value = SocketState.Failed(message)

        if (authFailureCount < MAX_AUTH_RETRIES) {
          authFailureCount = attempt
          coroutineScope.launch {
            val backoff = (2 * attempt).seconds
            ibark { "Retrying socket auth in $backoff" }
            delay(backoff)
            // Identity-change guard: if the logged-in user has changed during the backoff
            // window (logout, switch account, etc.) the captured `newSocket` and `userId` no
            // longer represent the current session. Reopening would re-auth a stale identity
            // — bail and let the new session's start() spin up a fresh socket instead.
            val currentUserId = (userSessionManager.current as? UserSession.LoggedIn)?.user?.id
            if (currentUserId != userId) {
              ibark { "User identity changed during retry backoff; aborting reconnect" }
              newSocket.close()
              return@launch
            }
            // The server just rejected the stored token — most likely it expired while the app
            // was backgrounded. Refresh it before reopening so the reconnect authenticates with
            // a live token instead of replaying the rejected one.
            val refreshed = tokenRefresher.refresh(userId, url, lastAuthToken)
            if (refreshed == null) {
              wbark { "Token refresh failed ahead of socket auth retry; retrying with stored token" }
            } else {
              ibark { "Token refreshed ahead of socket auth retry" }
            }
            // The refresh itself can invalidate the account (dead refresh token) and switch
            // sessions out from under us — re-check identity before reopening.
            val userIdAfterRefresh = (userSessionManager.current as? UserSession.LoggedIn)?.user?.id
            if (userIdAfterRefresh != userId) {
              ibark { "User identity changed during token refresh; aborting reconnect" }
              newSocket.close()
              return@launch
            }
            newSocket.close()
            newSocket.open()
          }
        } else {
          wbark { "Max auth retries reached; giving up. Token may be permanently invalid." }
        }
      }

      newSocket.on(Socket.EVENT_DISCONNECT) { args ->
        ibark { "Socket disconnected: ${args.joinToString()}" }
        // When the disconnect was triggered by the user disabling realtime sync, the
        // setting-observer has already parked state at Disabled. The socket's own disconnect
        // event fires async after close() — don't let it clobber the intentional Disabled state.
        if (_state.value !is SocketState.Disabled) {
          _state.value = SocketState.Disconnected
        }
      }

      newSocket.on(Socket.EVENT_CONNECT_ERROR) { args ->
        val reason = args.joinToString()
        ibark { "Socket connect error: $reason" }
        if (_state.value !is SocketState.Disabled) {
          _state.value = SocketState.Failed(reason)
        }
      }

      // No unconditional open(): the demand observer below opens the socket only while the
      // app is visible and the device has a network, so a process started for background
      // playback never dials the server.
      connectionJob?.cancel()
      connectionJob = coroutineScope.launch {
        launch { observeReachability(newSocket) }

        connectionDemand(
          lifecycle = appLifecycleObserver.state,
          connectivity = connectivity,
          inRange = serverReachability.observeInRange(url),
        ).collect { demanded ->
          socketDemanded = demanded
          if (demanded) {
            if (!settings.socketEnabled) return@collect
            if (!newSocket.connected) {
              ibark { "Socket demanded (foreground + network in range); opening with a fresh backoff" }
              _state.value = SocketState.Connecting
              // close() first resets the manager's reconnect backoff, so regaining the network
              // or foregrounding the app attempts immediately instead of waiting out a delay
              // accrued while the server was unreachable.
              newSocket.close()
              newSocket.open()
            }
          } else {
            ibark { "Socket no longer demanded (background, no network, or server out of range); closing" }
            newSocket.close()
            if (_state.value !is SocketState.Disabled) {
              _state.value = SocketState.Disconnected
            }
          }
        }
      }
    }
  }

  /**
   * Tunes [socket]'s reconnect loop to the app-wide [ServerReachability]: while the server is
   * known to be unreachable every attempt waits the maximum delay, and the moment another path
   * (an HTTP probe, a network change) finds it reachable again the socket reconnects immediately
   * instead of waiting out its backoff.
   */
  private suspend fun observeReachability(socket: Socket) {
    combine(
      reachabilitySignals(serverReachability.status),
      devSettings.observeAdaptToUnreachableServer(),
    ) { signal, adapt ->
      // The developer switch restores plain socket.io backoff
      if (adapt || signal == ReachabilitySignal.Reconnect) signal else ReachabilitySignal.Fast
    }.collect { signal ->
      when (signal) {
        ReachabilitySignal.Slow -> socket.io.setDelays(UNREACHABLE_DELAY_MS, UNREACHABLE_DELAY_MAX_MS)
        ReachabilitySignal.Fast -> socket.io.setDelays(RECONNECTION_DELAY_MS, RECONNECTION_DELAY_MAX_MS)
        ReachabilitySignal.Reconnect -> {
          socket.io.setDelays(RECONNECTION_DELAY_MS, RECONNECTION_DELAY_MAX_MS)
          if (socketDemanded && settings.socketEnabled && !socket.connected) {
            ibark { "Server reachable again; reconnecting socket with a fresh backoff" }
            _state.value = SocketState.Connecting
            socket.close()
            socket.open()
          }
        }
      }
    }
  }

  private fun Manager.setDelays(minMs: Long, maxMs: Long) {
    reconnectionDelay(minMs)
    reconnectionDelayMax(maxMs)
  }

  internal suspend fun stop() {
    connectionJob?.cancel()
    connectionJob = null
    socketDemanded = false
    val current = socket
    socket = null
    if (current != null) {
      ibark { "Closing socket connection" }
      current.close()
    }
    _state.value = SocketState.Disconnected
  }

  /** Same as [stop] but parks state at [SocketState.Disabled] for the indicator-hide path. */
  internal suspend fun stopForDisable() {
    stop()
    _state.value = SocketState.Disabled
  }

  override fun retryConnection() {
    if (!settings.socketEnabled) {
      ibark { "retryConnection called while socket is disabled; ignoring" }
      return
    }
    coroutineScope.launch {
      stop()
      val session = userSessionManager.current as? UserSession.LoggedIn
      if (session == null) {
        ibark { "retryConnection called without a logged-in session; ignoring" }
        return@launch
      }
      start(session)
    }
  }

  private fun Array<out Any?>.firstJsonElement(): JsonElement? {
    return firstOrNull() as? JsonElement
  }

  private fun Array<out Any?>.firstJsonObject(): JsonObject? {
    val element = firstJsonElement() ?: return null
    return runCatching { element.jsonObject }.getOrNull()
  }

  private fun JsonObject.string(key: String): String? {
    val element = this[key] ?: return null
    return runCatching { element.jsonPrimitive.contentOrNull }.getOrNull()
  }

  @ContributesMultibinding(UserScope::class, boundType = Scoped::class)
  @Inject
  class Lifecycle(
    private val socketManager: DefaultSocketManager,
    // The session this UserScope was built for. During login the global
    // UserSessionManager.current is still Loading when onCreate fires (changeSession only
    // publishes it after the graph is created), so reading the global here races and the
    // socket would silently never connect. The graph-bound session is always correct.
    private val userSession: UserSession,
  ) : Scoped {

    private var observerJob: Job? = null

    override suspend fun onCreate() {
      val session = userSession as? UserSession.LoggedIn ?: return
      // collectLatest cancels the previous start when the setting flips, so toggling off
      // mid-connection cleanly disconnects and toggling back on spins up a fresh socket. Editing
      // the custom headers restarts it too, so the new headers reach the WebSocket handshake.
      observerJob = socketManager.coroutineScope.launch {
        combine(
          socketManager.settings.observeSocketEnabled(),
          socketManager.accountManager.observeExtraHeaders(session.user.id),
        ) { enabled, headers -> enabled to headers }
          .distinctUntilChanged()
          .collectLatest { (enabled, _) ->
            if (enabled) {
              socketManager.stop()
              socketManager.start(session)
            } else {
              socketManager.stopForDisable()
            }
          }
      }
    }

    override suspend fun onDestroy() {
      // Cancel the settings observer so destroyed UserScopes don't accumulate collectors
      // on the app scope, each restarting the socket whenever the setting flips.
      observerJob?.cancel()
      observerJob = null
      socketManager.stop()
    }
  }
}
