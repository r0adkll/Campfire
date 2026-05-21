package app.campfire.socket.events

/**
 * Contributed by feature modules via `@ContributesMultibinding(UserScope::class, boundType = SocketEventListener::class)`.
 * The dispatcher in `:infra:socket:impl` launches one collector coroutine per listener and calls [handle] on every
 * event emitted by the socket. Implementations narrow the event themselves (`if (event !is X) return` or `when (event)`)
 * and are responsible for not throwing — the dispatcher catches and logs but does not retry.
 */
interface SocketEventListener {
  suspend fun handle(event: SocketEvent)
}
