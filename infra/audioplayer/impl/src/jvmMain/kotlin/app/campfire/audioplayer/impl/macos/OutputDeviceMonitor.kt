// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.macos

import app.campfire.core.logging.Cork
import com.sun.jna.Callback
import com.sun.jna.Function
import com.sun.jna.NativeLibrary
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.ptr.IntByReference

/** How the default output device is attached, from CoreAudio's `kAudioDevicePropertyTransportType`. */
enum class TransportType(internal val code: Int) {
  BuiltIn(fourCC("bltn")),
  Bluetooth(fourCC("blue")),
  BluetoothLE(fourCC("blea")),
  Usb(fourCC("usb ")),
  AirPlay(fourCC("airp")),
  Hdmi(fourCC("hdmi")),
  DisplayPort(fourCC("dprt")),
  Virtual(fourCC("virt")),
  Aggregate(fourCC("grup")),
  Unknown(0),
  ;

  /** A device that disappears when disconnected, so a fallback to the built-in speakers means it went away. */
  val isRemovable: Boolean get() = this == Bluetooth || this == BluetoothLE || this == Usb

  companion object {
    fun fromCode(code: Int): TransportType = entries.firstOrNull { it.code == code } ?: Unknown
  }
}

/**
 * Whether playback should pause after the default output moved from [previous] to [current]:
 * headphones or a speaker that were the output vanished and the Mac fell back to its own
 * speakers, matching what AVFoundation apps do on route loss.
 */
object OutputDevicePolicy {
  fun shouldPause(previous: TransportType, current: TransportType, playing: Boolean): Boolean {
    return playing && previous.isRemovable && current == TransportType.BuiltIn
  }
}

/**
 * Observes the system default output device through CoreAudio and reports transport changes.
 * The listener runs on a CoreAudio thread; consumers must hop to their own thread.
 */
internal class OutputDeviceMonitor(
  private val onDefaultOutputChanged: (previous: TransportType, current: TransportType) -> Unit,
) {

  @Structure.FieldOrder("selector", "scope", "element")
  class PropertyAddress(
    @JvmField var selector: Int = 0,
    @JvmField var scope: Int = 0,
    @JvmField var element: Int = 0,
  ) : Structure()

  fun interface PropertyListener : Callback {
    fun invoke(objectId: Int, addressCount: Int, addresses: Pointer?, clientData: Pointer?): Int
  }

  private val coreAudio: NativeLibrary = NativeLibrary.getInstance(CORE_AUDIO_FRAMEWORK)
  private val addListener: Function = coreAudio.getFunction("AudioObjectAddPropertyListener")
  private val removeListener: Function = coreAudio.getFunction("AudioObjectRemovePropertyListener")
  private val getPropertyData: Function = coreAudio.getFunction("AudioObjectGetPropertyData")

  private val defaultOutputAddress = PropertyAddress(PROPERTY_DEFAULT_OUTPUT, SCOPE_GLOBAL, ELEMENT_MAIN).also {
    it.write()
  }

  @Volatile
  private var current: TransportType = TransportType.Unknown

  /** Held for the life of the registration; JNA callbacks are only valid while referenced. */
  private val listener = PropertyListener { _, _, _, _ ->
    val previous = current
    val next = currentTransport()
    current = next
    if (next != previous) {
      ibark { "Default output changed: $previous -> $next" }
      onDefaultOutputChanged(previous, next)
    }
    NO_ERR
  }

  fun start() {
    current = currentTransport()
    ibark { "Default output is $current" }
    val status = addListener.invokeInt(arrayOf(SYSTEM_OBJECT, defaultOutputAddress, listener, null))
    check(status == NO_ERR) { "AudioObjectAddPropertyListener failed: $status" }
  }

  fun stop() {
    removeListener.invokeInt(arrayOf(SYSTEM_OBJECT, defaultOutputAddress, listener, null))
  }

  private fun currentTransport(): TransportType {
    val device = readInt(SYSTEM_OBJECT, defaultOutputAddress) ?: return TransportType.Unknown
    if (device == 0) return TransportType.Unknown
    val transport = PropertyAddress(PROPERTY_TRANSPORT_TYPE, SCOPE_GLOBAL, ELEMENT_MAIN).also { it.write() }
    return readInt(device, transport)?.let(TransportType::fromCode) ?: TransportType.Unknown
  }

  private fun readInt(objectId: Int, address: PropertyAddress): Int? {
    val size = IntByReference(Int.SIZE_BYTES)
    val out = IntByReference()
    val status = getPropertyData.invokeInt(arrayOf(objectId, address, 0, null, size, out))
    return if (status == NO_ERR) out.value else null
  }

  companion object : Cork {
    override val tag: String = "OutputDeviceMonitor"
    override val enabled: Boolean = true

    private const val CORE_AUDIO_FRAMEWORK = "/System/Library/Frameworks/CoreAudio.framework/CoreAudio"
    private const val NO_ERR = 0
    private const val SYSTEM_OBJECT = 1
    private const val ELEMENT_MAIN = 0
    private val SCOPE_GLOBAL = fourCC("glob")
    private val PROPERTY_DEFAULT_OUTPUT = fourCC("dOut")
    private val PROPERTY_TRANSPORT_TYPE = fourCC("tran")
  }
}

internal fun fourCC(code: String): Int {
  require(code.length == 4)
  return code.fold(0) { acc, c -> (acc shl 8) or (c.code and 0xff) }
}
