package app.campfire.audioplayer.impl.kvo

import app.campfire.audioplayer.impl.kvo.NSObservable.Option.Companion.asObserverOption
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSKeyValueObservingOptionInitial
import platform.Foundation.NSKeyValueObservingOptionNew
import platform.Foundation.NSKeyValueObservingOptionOld
import platform.Foundation.NSKeyValueObservingOptionPrior
import platform.Foundation.NSKeyValueObservingOptions
import platform.Foundation.addObserver
import platform.Foundation.removeObserver
import platform.darwin.NSObject
import platform.foundation.NSKeyValueObservingProtocol

typealias NSObservableAction<Obj> = (path: String?, obj: Obj, change: Map<Any?, *>?) -> Unit

@OptIn(ExperimentalForeignApi::class)
class NSObservable<Obj : NSObject>(
  private val nsObject: NSObject,
  private val path: String,
  private val options: List<Option> = listOf(Option.New),
  private val action: NSObservableAction<Obj>,
) : AutoCloseable {

  @Suppress("UNCHECKED_CAST")
  @OptIn(ExperimentalForeignApi::class)
  private val observer = object : NSObject(), NSKeyValueObservingProtocol {
    override fun observeValueForKeyPath(
      keyPath: String?,
      ofObject: Any?,
      change: Map<Any?, *>?,
      context: COpaquePointer?,
    ) {
      action(keyPath, ofObject as Obj, change)
    }
  }

  internal fun start() {
    nsObject.addObserver(
      observer = observer,
      forKeyPath = path,
      options = options.asObserverOption(),
      context = null,
    )
  }

  override fun close() {
    nsObject.removeObserver(
      observer = observer,
      forKeyPath = path,
    )
  }

  enum class Option(val option: ULong) {
    New(NSKeyValueObservingOptionNew),
    Old(NSKeyValueObservingOptionOld),
    Initial(NSKeyValueObservingOptionInitial),
    Prior(NSKeyValueObservingOptionPrior),
    ;

    companion object {
      internal fun List<Option>.asObserverOption(): NSKeyValueObservingOptions {
        return fold(0.toULong()) { acc, option ->
          acc or option.option
        }
      }
    }
  }
}

fun <Obj : NSObject> Obj.observe(
  path: String,
  options: List<NSObservable.Option> = listOf(NSObservable.Option.New),
  action: NSObservableAction<Obj>,
): NSObservable<Obj> {
  return NSObservable(this, path, options, action).apply {
    start()
  }
}
