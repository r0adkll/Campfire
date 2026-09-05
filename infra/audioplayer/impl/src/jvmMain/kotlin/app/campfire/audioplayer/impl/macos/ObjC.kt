// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.macos

import com.sun.jna.Callback
import com.sun.jna.Function
import com.sun.jna.NativeLibrary
import com.sun.jna.Pointer

/**
 * Minimal Objective-C runtime bridge over JNA — just enough to message existing classes and to
 * define a small target class whose methods are implemented by JNA callbacks.
 *
 * `objc_msgSend` is bound as a fixed-arity function and invoked with exact argument arrays, never
 * as C varargs: on arm64 the variadic calling convention differs and a varargs binding would pass
 * arguments in the wrong registers.
 */
internal object ObjC {

  private val objc: NativeLibrary = NativeLibrary.getInstance("objc")
  private val msgSend: Function = objc.getFunction("objc_msgSend")
  private val getClass: Function = objc.getFunction("objc_getClass")
  private val registerName: Function = objc.getFunction("sel_registerName")
  private val allocateClassPair: Function = objc.getFunction("objc_allocateClassPair")
  private val registerClassPair: Function = objc.getFunction("objc_registerClassPair")
  private val addMethod: Function = objc.getFunction("class_addMethod")
  private val poolPush: Function = objc.getFunction("objc_autoreleasePoolPush")
  private val poolPop: Function = objc.getFunction("objc_autoreleasePoolPop")

  fun cls(name: String): Pointer = getClass.invokePointer(arrayOf(name)) ?: error("Objective-C class $name not found")

  fun sel(name: String): Pointer = registerName.invokePointer(arrayOf(name))

  fun send(receiver: Pointer, selector: String, vararg args: Any?): Pointer? =
    msgSend.invokePointer(arrayOf(receiver, sel(selector), *args))

  fun sendLong(receiver: Pointer, selector: String, vararg args: Any?): Long =
    msgSend.invokeLong(arrayOf(receiver, sel(selector), *args))

  fun sendDouble(receiver: Pointer, selector: String, vararg args: Any?): Double =
    msgSend.invokeDouble(arrayOf(receiver, sel(selector), *args))

  fun sendVoid(receiver: Pointer, selector: String, vararg args: Any?) {
    msgSend.invokeVoid(arrayOf(receiver, sel(selector), *args))
  }

  /** Runs [block] inside an autorelease pool so objects returned by class methods are released. */
  inline fun <T> autoreleased(block: () -> T): T {
    val pool = poolPush.invokePointer(emptyArray())
    try {
      return block()
    } finally {
      poolPop.invokeVoid(arrayOf(pool))
    }
  }

  /** An instance method implemented in Kotlin: [types] is the ObjC type encoding, e.g. `q@:@`. */
  class Method(val selector: String, val types: String, val imp: Callback)

  /**
   * Defines (or returns the already-defined) class [name] extending [superclass] with [methods].
   * Callers must keep the [Method.imp] callbacks strongly referenced for the life of the class.
   */
  fun defineClass(name: String, superclass: String, methods: List<Method>): Pointer {
    getClass.invokePointer(arrayOf(name))?.let { return it }
    val cls = allocateClassPair.invokePointer(arrayOf(cls(superclass), name, 0L))
      ?: error("Unable to allocate Objective-C class $name")
    methods.forEach { method ->
      addMethod.invokeInt(arrayOf(cls, sel(method.selector), method.imp, method.types))
    }
    registerClassPair.invokeVoid(arrayOf(cls))
    return cls
  }

  /** The value of an exported `NSString *const` (or other pointer-sized) global in [library]. */
  fun global(library: NativeLibrary, symbol: String): Pointer =
    library.getGlobalVariableAddress(symbol).getPointer(0) ?: error("$symbol is null")

  fun nsString(value: String): Pointer =
    send(cls("NSString"), "stringWithUTF8String:", value) ?: error("NSString allocation failed")

  fun nsNumber(value: Double): Pointer =
    send(cls("NSNumber"), "numberWithDouble:", value) ?: error("NSNumber allocation failed")

  fun nsNumber(value: Long): Pointer =
    send(cls("NSNumber"), "numberWithLongLong:", value) ?: error("NSNumber allocation failed")

  fun nsArray(single: Pointer): Pointer =
    send(cls("NSArray"), "arrayWithObject:", single) ?: error("NSArray allocation failed")

  fun nsMutableDictionary(): Pointer =
    send(cls("NSMutableDictionary"), "dictionary") ?: error("NSMutableDictionary allocation failed")

  fun Pointer.put(key: Pointer, value: Pointer) = sendVoid(this, "setObject:forKey:", value, key)

  /** Reads an NSString as a Kotlin string. */
  fun Pointer.utf8(): String? = send(this, "UTF8String")?.getString(0, "UTF-8")
}
