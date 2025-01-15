package app.campfire.audioplayer.impl.kvo

import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSNotificationName
import platform.darwin.NSObject
import platform.darwin.NSObjectProtocol

typealias NSNotificationObservableAction<Obj> = (obj: Obj, notification: NSNotification?) -> Unit

class NSNotificationObservable<Obj : NSObject>(
  private val notificationCenter: NSNotificationCenter,
  private val nsObject: Obj,
  private val name: NSNotificationName,
  private val action: NSNotificationObservableAction<Obj>,
) : AutoCloseable {

  private var observer: NSObjectProtocol? = null

  internal fun start() {
    observer = notificationCenter.addObserverForName(
      name = name,
      `object` = nsObject,
      queue = null,
      usingBlock = { notification -> action(nsObject, notification) },
    )
  }

  override fun close() {
    observer?.let { notificationCenter.removeObserver(it) }
  }
}

fun <Obj : NSObject> NSNotificationCenter.observe(
  name: NSNotificationName,
  nsObject: Obj,
  action: NSNotificationObservableAction<Obj>,
): NSNotificationObservable<Obj> {
  return NSNotificationObservable(
    notificationCenter = this,
    nsObject = nsObject,
    name = name,
    action = action,
  ).apply { start() }
}
