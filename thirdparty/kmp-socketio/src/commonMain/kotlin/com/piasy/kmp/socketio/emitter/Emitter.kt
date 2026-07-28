package com.piasy.kmp.socketio.emitter

import com.piasy.kmp.socketio.engineio.CallerThread

/**
 * Created by Piasy{github.com/Piasy} on 2022/11/16.
 *
 * The event emitter which is ported from the Java module.
 *
 * This class is NOT thread-safe.
 * Don't call on/once/off inside event callback, it may trigger ConcurrentModificationException.
 */
open class Emitter {
    interface Listener {
        fun call(vararg args: Any)
    }

    private val callbacks = mutableMapOf<String, MutableList<Listener>>()

    // to avoid concurrent modification error, we use two collections of listener.
    private val onceCallbacks = mutableMapOf<String, MutableList<Listener>>()

    /**
     * Listens on the event.
     * @param event event name.
     * @param fn
     * @return a reference to this object.
     */
    @CallerThread
    fun on(event: String, fn: Listener): Emitter {
        addListener(callbacks, event, fn)
        return this
    }

    @CallerThread
    fun on(event: String, block: (Array<out Any>) -> Unit): Emitter {
        return on(event, object : Listener {
            override fun call(vararg args: Any) {
                block(args)
            }
        })
    }

    private fun addListener(
        callbacks: MutableMap<String, MutableList<Listener>>,
        event: String,
        fn: Listener
    ) {
        callbacks.getOrElse(event) {
            val listeners = mutableListOf<Listener>()
            callbacks[event] = listeners
            listeners
        }.add(fn)
    }

    /**
     * Adds a one time listener for the event.
     *
     * @param event an event name.
     * @param fn
     * @return a reference to this object.
     */
    @CallerThread
    fun once(event: String, fn: Listener): Emitter {
        addListener(onceCallbacks, event, fn)
        return this
    }

    @CallerThread
    fun once(event: String, block: (Array<out Any>) -> Unit): Emitter {
        return once(event, object : Listener {
            override fun call(vararg args: Any) {
                block(args)
            }
        })
    }

    /**
     * Removes all registered listeners.
     *
     * @return a reference to this object.
     */
    @CallerThread
    fun off(): Emitter {
        callbacks.clear()
        onceCallbacks.clear()
        return this
    }

    /**
     * Removes all listeners of the specified event.
     *
     * @param event an event name.
     * @return a reference to this object.
     */
    @CallerThread
    fun off(event: String): Emitter {
        callbacks.remove(event)
        onceCallbacks.remove(event)
        return this
    }

    /**
     * Removes the listener.
     *
     * @param event an event name.
     * @param fn
     * @return a reference to this object.
     */
    @CallerThread
    fun off(event: String, fn: Listener): Emitter {
        callbacks[event]?.remove(fn)
        onceCallbacks[event]?.remove(fn)
        return this
    }

    /**
     * Executes each of listeners with the given args.
     *
     * @param event an event name.
     * @param args
     * @return a reference to this object.
     */
    @CallerThread
    open fun emit(event: String, vararg args: Any): Emitter {
        val actions = ArrayList<() -> Unit>()
        callbacks[event]?.forEach {
            actions.add { it.call(*args) }
        }

        onceCallbacks.remove(event)?.forEach {
            actions.add { it.call(*args) }
        }

        for (action in actions) {
            action()
        }

        return this
    }

    /**
     * Returns a list of listeners for the specified event.
     *
     * @param event an event name.
     * @return a reference to this object.
     */
    @CallerThread
    fun listeners(event: String): List<Listener> {
        val listeners = mutableListOf<Listener>()
        listeners.addAll(callbacks[event].orEmpty())
        listeners.addAll(onceCallbacks[event].orEmpty())
        return listeners
    }

    /**
     * Check if this emitter has listeners for the specified event.
     *
     * @param event an event name.
     * @return a reference to this object.
     */
    @CallerThread
    fun hasListeners(event: String): Boolean {
        return !(callbacks[event].isNullOrEmpty() && onceCallbacks[event].isNullOrEmpty())
    }
}
