package app.campfire.socket.impl

import app.campfire.account.api.AccountManager
import app.campfire.account.api.UserSessionManager
import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.di.Scoped
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.logging.Corked
import app.campfire.core.session.UserSession
import app.campfire.network.di.UserClient
import app.campfire.socket.RawSocketEvent
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
import com.piasy.kmp.socketio.socketio.IO
import com.piasy.kmp.socketio.socketio.Socket
import com.piasy.kmp.xlog.Logging
import com.piasy.kmp.xlog.LoggingImpl
import com.r0adkll.kimchi.annotations.ContributesBinding
import com.r0adkll.kimchi.annotations.ContributesMultibinding
import io.ktor.client.HttpClient
import kotlin.concurrent.Volatile
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import me.tatarka.inject.annotations.Inject

@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class)
@Inject
class DefaultSocketManager(
  private val userSessionManager: UserSessionManager,
  private val accountManager: AccountManager,
  @UserClient private val httpClient: HttpClient,
  @ForScope(UserScope::class) private val coroutineScopeHolder: CoroutineScopeHolder,
) : SocketManager {

  companion object : Corked("DefaultSocketManager") {
    private const val MAX_AUTH_RETRIES = 3
  }

  @Volatile
  private var authFailureCount: Int = 0

  private val _state = MutableStateFlow<SocketState>(SocketState.Disconnected)
  override val state: StateFlow<SocketState> = _state.asStateFlow()

  private val _rawEvents = MutableSharedFlow<RawSocketEvent>(extraBufferCapacity = 64)
  override val rawEvents: SharedFlow<RawSocketEvent> = _rawEvents.asSharedFlow()

  private val _events = MutableSharedFlow<SocketEvent>(extraBufferCapacity = 64)
  override val events: SharedFlow<SocketEvent> = _events.asSharedFlow()

  private val json = Json {
    ignoreUnknownKeys = true
  }

  /**
   * A managed list of events we can handle and parse from socket.io
   */
  private val handlers = listOf(
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

  internal suspend fun start() {
    val session = userSessionManager.current as? UserSession.LoggedIn ?: return
    if (accountManager.getToken(session.user.id)?.accessToken == null) {
      wbark { "No access token for user ${session.user.id}; skipping socket connect" }
      return
    }

    val userId = session.user.id
    val url = session.user.serverUrl
    _state.value = SocketState.Connecting

    Logging.init(object : LoggingImpl {
      override fun debug(): Boolean {
        return false
      }

      override fun debug(tag: String, content: String) {
        vbark { content }
      }

      override fun info(tag: String, content: String) {
        dbark { content }
      }

      override fun error(tag: String, content: String) {
        ebark { content }
      }
    })

    val opts = IO.Options().apply {
      httpClient = this@DefaultSocketManager.httpClient
    }
    IO.socket(url, opts) { newSocket ->
      socket = newSocket

      newSocket.on(Socket.EVENT_CONNECT) {
        ibark { "Socket connected to $url; reading token and sending auth" }
        _state.value = SocketState.Authenticating
        coroutineScopeHolder.get().launch {
          val freshToken = accountManager.getToken(userId)?.accessToken
          if (freshToken != null) {
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
          ibark { "Socket authenticated as $username ($authedUserId)" }
          _state.value = SocketState.Authenticated(authedUserId, username)
        }
        _rawEvents.tryEmit(RawSocketEvent("init", args.toList()))
      }

      handlers.forEach { handler ->
        newSocket.on(handler.name) { args ->
          val element = args.firstJsonElement()
          if (element != null) {
            runCatching { with(handler) { json.decode(element) } }
              .onSuccess { _events.tryEmit(it) }
              .onFailure { wbark { "Failed to parse ${handler.name}: ${it.message}" } }
          }
          _rawEvents.tryEmit(RawSocketEvent(handler.name, args.toList()))
        }
      }

      newSocket.on("auth_failed") { args ->
        val message = args.firstJsonObject()?.string("message") ?: "Auth failed"
        val attempt = authFailureCount + 1
        wbark { "Socket auth failed: $message (attempt $attempt/$MAX_AUTH_RETRIES)" }
        _state.value = SocketState.Failed(message)
        _rawEvents.tryEmit(RawSocketEvent("auth_failed", args.toList()))

        if (authFailureCount < MAX_AUTH_RETRIES) {
          authFailureCount = attempt
          coroutineScopeHolder.get().launch {
            val backoff = (2 * attempt).seconds
            ibark { "Retrying socket auth in $backoff" }
            delay(backoff)
            newSocket.close()
            newSocket.open()
          }
        } else {
          wbark { "Max auth retries reached; giving up. Token may be permanently invalid." }
        }
      }

      newSocket.on(Socket.EVENT_DISCONNECT) { args ->
        ibark { "Socket disconnected: ${args.joinToString()}" }
        _state.value = SocketState.Disconnected
      }

      newSocket.on(Socket.EVENT_CONNECT_ERROR) { args ->
        val reason = args.joinToString()
        ibark { "Socket connect error: $reason" }
        _state.value = SocketState.Failed(reason)
      }

      newSocket.open()
    }
  }

  internal suspend fun stop() {
    val current = socket
    socket = null
    if (current != null) {
      ibark { "Closing socket connection" }
      current.close()
    }
    _state.value = SocketState.Disconnected
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
  ) : Scoped {
    override suspend fun onCreate() {
      socketManager.start()
    }

    override suspend fun onDestroy() {
      socketManager.stop()
    }
  }
}
