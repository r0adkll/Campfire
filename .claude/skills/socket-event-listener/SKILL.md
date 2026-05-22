---
name: socket-event-listener
description: Enforce the Campfire pattern for feature modules to react to Audiobookshelf socket events. Feature impl modules contribute a `SocketEventListener` via `@ContributesMultibinding(UserScope::class, boundType = SocketEventListener::class)` — no `Scoped`, no `CoroutineScopeHolder` injection, no flow collection, no edits to `:infra:socket:impl`. Trigger when authoring or reviewing a class that reacts to socket events, or when asked to "wire X to socket events" / "add a socket listener for Y".
---

## Rule

**Feature modules subscribe to socket events by contributing a `SocketEventListener` from their own `impl` module. The central dispatcher in `:infra:socket:impl` owns all collection, lifecycle, and error-handling — listeners are pure business logic.**

```kotlin
// :features:user:impl/.../MediaProgressSocketListener.kt
@ContributesMultibinding(UserScope::class, boundType = SocketEventListener::class)
@Inject
class MediaProgressSocketListener(
  private val mediaProgressRepository: MediaProgressRepository,
) : SocketEventListener {
  override suspend fun handle(event: SocketEvent) {
    when (event) {
      is UserItemProgressUpdated -> mediaProgressRepository.upsert(event.payload.data)
      else -> Unit
    }
  }
}
```

That's the whole shape. No init blocks, no coroutine launching, no `socketManager` injection.

## Why

- **Separation of concerns.** Socket transport / reconnect / token-refresh / app-lifecycle plumbing lives in `:infra:socket:impl`. Feature reactions live with the feature they belong to.
- **Zero boilerplate per listener.** Dispatcher injects `Set<SocketEventListener>` and launches one collector coroutine per listener at UserScope creation. New consumers ship in one file.
- **Error isolation.** Dispatcher wraps every `handle()` in `runCatching` and logs failures — a single bad event can't permanently kill a listener.
- **Discoverability.** All consumers are findable by grepping `: SocketEventListener` across the repo.

## Where the listener lives

**In the feature module that owns the domain it's updating.** Examples:

| Event(s) | Owning module |
|---|---|
| `UserItemProgressUpdated`, `UserSessionClosed` | `:features:user:impl` |
| `ItemAdded`, `ItemUpdated`, `ItemRemoved`, `ItemsAdded`, `ItemsUpdated`, `LibraryAdded`, `LibraryUpdated`, `LibraryRemoved` | `:features:libraries:impl` |
| `SeriesAdded`/`Updated`/`Removed` | `:features:series:impl` |
| `CollectionAdded`/`Updated`/`Removed` | `:features:collections:impl` |
| `AuthorAdded`/`Updated`/`Removed` | `:features:author:impl` |
| `PlaylistAdded`/`Updated`/`Removed` | `:features:playlists:impl` |
| `EpisodeAdded`, `EpisodeDownload*` | `:features:podcasts:impl` |
| `UserUpdated` | `:data:account:impl` |
| `NotificationsUpdated` | wherever notification settings are managed |

A feature that touches multiple event families is fine — one listener class can `when`-switch over several events. Don't make multiple listener classes just to fragment a `when` block.

## Implementation rules

1. **Annotate** with `@ContributesMultibinding(UserScope::class, boundType = SocketEventListener::class)` and `@Inject`. Without the annotation kimchi won't register the binding and the dispatcher silently won't call it.
2. **Inject only domain dependencies** (the repos / stores / sync services you'll write to). Do NOT inject `SocketManager`, `CoroutineScopeHolder`, or anything socket-related.
3. **Narrow via `when` or early-return `if !is`.** Every listener gets every event — that's intentional. Return `Unit` for events you don't care about (or omit the `else` branch if your `when` is exhaustive over the events you handle).
4. **Keep `handle()` fast.** It runs sequentially per listener — a 500ms `handle` delays the next event for that listener. For genuinely slow work, dispatch to a separate component or coroutine (rare in practice; repo upserts are sub-millisecond).
5. **Don't catch exceptions just to swallow them.** The dispatcher already wraps `handle()` in `runCatching` and logs failures with the listener class name and event. If you have invariants that must hold, throw — you'll see it in logs.

## When authoring a *new event type* (not a listener)

If you're adding a new `SocketEvent` (not consuming one), one rule travels with it:

**If the payload extends `app.campfire.network.models.NetworkModel` or `app.campfire.network.envelopes.Envelope`, override `applyOrigin(origin: RequestOrigin)` and propagate to the payload.** The dispatcher calls `event.applyOrigin(RequestOrigin.Url(serverUrl))` immediately after decoding so downstream DB inserts (which inspect `NetworkModel.origin` for cover URLs, signed-stream headers, etc.) see the right server. The default on `SocketEvent` is a no-op, so forgetting silently loses metadata — DB rows end up with `RequestOrigin.None` and any code that builds absolute URLs from `origin` breaks.

```kotlin
// ✅ Single object payload
data class LibraryUpdated(val library: Library) : SocketEvent {
  override fun applyOrigin(origin: RequestOrigin) = library.applyOrigin(origin)
  /* ... */
}

// ✅ List payload — fan out
data class ItemsUpdated(val items: List<LibraryItemExpanded>) : SocketEvent {
  override fun applyOrigin(origin: RequestOrigin) = items.forEach { it.applyOrigin(origin) }
  /* ... */
}

// ✅ Envelope wrapping a NetworkModel — reach through
data class UserItemProgressUpdated(val payload: UserItemProgressUpdatedPayload) : SocketEvent {
  override fun applyOrigin(origin: RequestOrigin) = payload.data.applyOrigin(origin)
  /* ... */
}

// ✅ Plain payloads (Series, User, PodcastEpisode, your own *Payload types) — no override needed.
data class SeriesAdded(val series: Series) : SocketEvent { /* ... */ }
```

**Audit rule of thumb**: if the payload field's type extends `NetworkModel` (directly or via `Envelope`), the event needs an `applyOrigin` override. Grep:

```bash
rg -l ': NetworkModel\(\)|: Envelope\(\)' data/network/api --type kt
```

…and check that every event whose payload type appears in that list has the override.

## Anti-patterns

- **Adding the listener in `:infra:socket:impl`.** Don't. Socket impl is generic plumbing only; the only listener that lives there is the universal `SocketEventLogger`.

  ```kotlin
  // ❌ DON'T: infra/socket/impl/.../MyFeatureListener.kt
  ```

- **Implementing `Scoped` and rolling your own collect.** This was the pre-dispatcher pattern. With the dispatcher, it duplicates work and creates a second collector subscribed to the same SharedFlow.

  ```kotlin
  // ❌ DON'T
  @ContributesMultibinding(UserScope::class, boundType = Scoped::class)
  class MyFeatureListener(
    private val socketManager: SocketManager,
    @ForScope(UserScope::class) private val scopeHolder: CoroutineScopeHolder,
    private val repo: MyRepo,
  ) : Scoped {
    override suspend fun onCreate() {
      scopeHolder.get().launch {
        socketManager.events.filterIsInstance<MyEvent>().collect { repo.upsert(it) }
      }
    }
  }
  ```

- **Injecting `SocketManager` directly.** The point of the dispatcher is that listeners never see the socket — they get events handed to them. Injecting it means you're about to bypass the dispatcher.

- **One listener per event type.** Five `Removed` events from the library family don't need five listener classes. One `LibraryCacheInvalidator : SocketEventListener` with a `when` block covering all of them is clearer and bundles related cache invalidation into one place.

  ```kotlin
  // ✅ Preferred — one listener, multi-event when
  override suspend fun handle(event: SocketEvent) {
    when (event) {
      is ItemAdded, is ItemUpdated, is ItemRemoved,
      is ItemsAdded, is ItemsUpdated -> libraryStore.invalidate()
      is LibraryAdded, is LibraryUpdated, is LibraryRemoved -> libraryListStore.invalidate()
      else -> Unit
    }
  }
  ```

- **Throwing as a signal.** `throw IgnoreEvent()` to skip an event is wrong — just `return`/`Unit`. Throws go to the dispatcher's error log.

- **Mutating shared state without synchronization.** Multiple listeners run in parallel coroutines. If two listeners write to the same in-memory cache, you need a Mutex or atomic primitives. Most repos / Stores already handle this internally — but if you're rolling your own, beware.

## Concurrency model (so you don't get surprised)

- One long-lived coroutine per listener, launched at UserScope creation, cancelled at logout.
- Each listener has its own `socketManager.events.collect`. Events flow into all listeners concurrently.
- Within a listener, `handle()` calls are sequential — event N must finish before event N+1 starts for that listener.
- The underlying `MutableSharedFlow` has `extraBufferCapacity = 64`. If your listener falls behind by >64 events, `tryEmit` upstream starts returning `false` and events get dropped at the source for *all* listeners. In practice this never happens for socket events, but don't do anything in `handle()` that takes seconds.

## When to apply

- Authoring a class that reacts to ABS socket events → use this pattern.
- User asks "wire `<feature>` to react to `<event>` updates" → drop a `SocketEventListener` impl in the feature's impl module.
- Reviewing a PR that touches `:infra:socket:impl` to add feature-specific reactions → push back; that work belongs in the feature module.
- Migrating an existing manual `events.filterIsInstance<X>().collect { ... }` subscriber (if any exist) → consolidate it into a `SocketEventListener`.

## Auditing the codebase

```bash
# Find all socket consumers
rg -l ': SocketEventListener' --type kt -g '!*/build/*'

# Find manual subscribers that should be migrated to the dispatcher
rg 'socketManager\.events' --type kt -g '!*/build/*' -g '!*/infra/socket/*'
```

The latter should return zero hits outside `:infra:socket:impl` (the dispatcher itself) and the test module. Any hit elsewhere is a pre-dispatcher subscriber that should be refactored to a `SocketEventListener`.
