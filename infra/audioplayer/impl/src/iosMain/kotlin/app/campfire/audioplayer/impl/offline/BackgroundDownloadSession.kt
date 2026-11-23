// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.offline

import app.campfire.core.logging.Cork
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import platform.Foundation.NSError
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.Foundation.NSURLErrorCancelled
import platform.Foundation.NSURLErrorDomain
import platform.Foundation.NSURLRequest
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.NSURLSessionDownloadDelegateProtocol
import platform.Foundation.NSURLSessionDownloadTask
import platform.Foundation.NSURLSessionTask
import platform.Foundation.NSURLSessionTaskStateRunning
import platform.Foundation.NSURLSessionTaskStateSuspended
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * The app's one background `URLSession`. iOS runs its transfers on the app's behalf, so a download
 * carries on while the app is suspended, and even after the system terminates it: iOS then
 * relaunches the app in the background to hand over the finished file
 * ([handleEventsForBackgroundURLSession]).
 *
 * Each task is tagged ([TransferTag]) with the `.part` file it fills, relative to the downloads
 * directory (whose absolute path changes when the app is updated), and the byte offset its request
 * resumes from. A file that lands while nothing waits for it, as after a relaunch, still ends up in
 * its part file, where the queue finds it on its next attempt. All of the session's bookkeeping
 * runs on its serial delegate queue.
 */
@OptIn(ExperimentalForeignApi::class)
object BackgroundDownloadSession : Cork {

  override val tag: String = "BackgroundDownloads"
  override val enabled: Boolean = true

  private const val IDENTIFIER = "app.campfire.downloads"

  /** A response whose body couldn't be saved to its part file. */
  internal const val STATUS_NOT_SAVED = -1

  private val delegateQueue = NSOperationQueue().apply {
    name = "app.campfire.downloads.session"
    maxConcurrentOperationCount = 1
  }

  private val session: NSURLSession by lazy {
    val configuration = NSURLSessionConfiguration.backgroundSessionConfigurationWithIdentifier(IDENTIFIER).apply {
      sessionSendsLaunchEvents = true
      discretionary = false
    }
    NSURLSession.sessionWithConfiguration(configuration, SessionDelegate(), delegateQueue)
  }

  // Only touched on delegateQueue
  private val transfers = mutableMapOf<ULong, Transfer>()
  private val savedStatus = mutableMapOf<ULong, Int>()
  private val unclaimed = mutableMapOf<ULong, TransferOutcome>()
  private var eventsCompletionHandler: (() -> Unit)? = null

  /**
   * Called from the app delegate when iOS relaunches (or wakes) the app because this session's
   * transfers finished. Reconnecting the session delivers them; [completionHandler] runs once
   * they've all been handled. Returns false for another session.
   */
  fun handleEventsForBackgroundURLSession(identifier: String, completionHandler: () -> Unit): Boolean {
    if (identifier != IDENTIFIER) return false
    ibark { "Relaunched to deliver finished downloads" }
    delegateQueue.addOperationWithBlock { eventsCompletionHandler = completionHandler }
    connect()
    return true
  }

  /** Reconnects to transfers that carried on while the app wasn't running. */
  internal fun connect() {
    session
  }

  /** The unfinished task filling [relativePath], from this launch or one before it. */
  internal suspend fun runningTask(relativePath: String): Pair<NSURLSessionDownloadTask, TransferTag>? {
    val tasks = suspendCancellableCoroutine { continuation ->
      session.getAllTasksWithCompletionHandler { tasks -> continuation.resume(tasks.orEmpty()) }
    }
    return tasks.filterIsInstance<NSURLSessionDownloadTask>().firstNotNullOfOrNull { task ->
      val tag = TransferTag.decode(task.taskDescription)
      val unfinished = task.state == NSURLSessionTaskStateRunning || task.state == NSURLSessionTaskStateSuspended
      if (tag?.relativePath == relativePath && unfinished) task to tag else null
    }
  }

  internal fun newTask(request: NSURLRequest, tag: TransferTag): NSURLSessionDownloadTask =
    session.downloadTaskWithRequest(request).apply { taskDescription = tag.encode() }

  /**
   * Runs [task] (if it isn't already) and waits for it to finish. Cancelling the wait cancels the
   * task. [onProgress] gets (bytes in the part file, total bytes or <= 0 when unknown).
   */
  internal suspend fun await(task: NSURLSessionDownloadTask, onProgress: (Long, Long) -> Unit): TransferOutcome {
    val id = task.taskIdentifier
    val transfer = Transfer(task, onProgress)
    delegateQueue.addOperationWithBlock {
      val finished = unclaimed.remove(id)
      if (finished != null) transfer.outcome.complete(finished) else transfers[id] = transfer
      task.resume()
    }
    return try {
      transfer.outcome.await()
    } catch (e: CancellationException) {
      task.cancel()
      delegateQueue.addOperationWithBlock { transfers.remove(id) }
      throw e
    }
  }

  private fun onWrite(task: NSURLSessionDownloadTask, written: Long, expected: Long) {
    val transfer = transfers[task.taskIdentifier] ?: return
    val tag = TransferTag.decode(task.taskDescription) ?: return
    // A server that ignores the range sends the whole file from the start
    val base = if (task.httpStatus() == HTTP_PARTIAL_CONTENT) tag.offset else 0L
    transfer.onProgress(base + written, if (expected > 0) base + expected else -1L)
  }

  /** Saves the body into its part file. iOS deletes [location] as soon as this returns. */
  private fun onDownloaded(task: NSURLSessionDownloadTask, location: NSURL) {
    val tag = TransferTag.decode(task.taskDescription) ?: return
    val status = task.httpStatus()
    val part = Path(iosDownloadsRoot(), tag.relativePath)
    val body = location.path?.let(::Path)
    savedStatus[task.taskIdentifier] = try {
      when {
        body == null -> STATUS_NOT_SAVED
        status == HTTP_OK -> {
          body.moveOnto(part)
          status
        }
        status == HTTP_PARTIAL_CONTENT && part.length() == tag.offset -> {
          SystemFileSystem.source(body).buffered().use { source ->
            SystemFileSystem.sink(part, append = true).buffered().use { sink -> source.transferTo(sink) }
          }
          status
        }
        // The part file changed since the request was made
        status == HTTP_PARTIAL_CONTENT -> STATUS_NOT_SAVED
        else -> status
      }
    } catch (e: Exception) {
      ebark(e) { "Unable to save a finished download to ${tag.relativePath}" }
      STATUS_NOT_SAVED
    }
  }

  private fun onComplete(task: NSURLSessionTask, error: NSError?) {
    val id = task.taskIdentifier
    val status = savedStatus.remove(id) ?: task.httpStatus()
    val cancelled = error?.domain == NSURLErrorDomain && error?.code == NSURLErrorCancelled
    val outcome = if (error != null) {
      TransferOutcome.Failed(error.localizedDescription, cancelled)
    } else {
      TransferOutcome.Finished(status, (task.response as? NSHTTPURLResponse)?.totalBytes() ?: -1L)
    }
    val transfer = transfers.remove(id) ?: supersededTransfer(task, outcome)
    when {
      transfer != null -> transfer.outcome.complete(outcome)
      !cancelled -> unclaimed[id] = outcome
    }
  }

  /**
   * After a relaunch, the queue can resume a file before iOS has delivered the transfer from the
   * earlier launch that already finished it, and so start a second transfer for the same part file.
   * When the earlier one's body lands, it stands in for that second transfer, which is cancelled
   * rather than downloading the file again.
   */
  private fun supersededTransfer(task: NSURLSessionTask, outcome: TransferOutcome): Transfer? {
    if (outcome !is TransferOutcome.Finished) return null
    if (outcome.status != HTTP_OK && outcome.status != HTTP_PARTIAL_CONTENT) return null
    val relativePath = TransferTag.decode(task.taskDescription)?.relativePath ?: return null
    val (id, transfer) = transfers.entries
      .firstOrNull { TransferTag.decode(it.value.task.taskDescription)?.relativePath == relativePath }
      ?.toPair()
      ?: return null
    ibark { "A transfer from an earlier launch finished $relativePath, cancelling its replacement" }
    transfers.remove(id)
    transfer.task.cancel()
    return transfer
  }

  private fun onEventsFinished() {
    val handler = eventsCompletionHandler ?: return
    eventsCompletionHandler = null
    dispatch_async(dispatch_get_main_queue()) { handler() }
  }

  private fun NSURLSessionTask.httpStatus(): Int = (response as? NSHTTPURLResponse)?.statusCode?.toInt() ?: 0

  /** The whole file's size: `N` in `Content-Range: bytes a-b/N` (or `*\/N`), else a full response's length. */
  private fun NSHTTPURLResponse.totalBytes(): Long =
    valueForHTTPHeaderField("Content-Range")?.substringAfterLast('/')?.toLongOrNull()
      ?: expectedContentLength.takeIf { statusCode.toInt() == HTTP_OK && it > 0 }
      ?: -1L

  private class Transfer(val task: NSURLSessionDownloadTask, val onProgress: (Long, Long) -> Unit) {
    val outcome = CompletableDeferred<TransferOutcome>()
  }

  private class SessionDelegate : NSObject(), NSURLSessionDownloadDelegateProtocol {
    override fun URLSession(
      session: NSURLSession,
      downloadTask: NSURLSessionDownloadTask,
      didFinishDownloadingToURL: NSURL,
    ) = onDownloaded(downloadTask, didFinishDownloadingToURL)

    override fun URLSession(
      session: NSURLSession,
      downloadTask: NSURLSessionDownloadTask,
      didWriteData: Long,
      totalBytesWritten: Long,
      totalBytesExpectedToWrite: Long,
    ) = onWrite(downloadTask, totalBytesWritten, totalBytesExpectedToWrite)

    override fun URLSession(session: NSURLSession, task: NSURLSessionTask, didCompleteWithError: NSError?) =
      onComplete(task, didCompleteWithError)

    override fun URLSessionDidFinishEventsForBackgroundURLSession(session: NSURLSession) = onEventsFinished()
  }

  internal const val HTTP_OK = 200
  internal const val HTTP_PARTIAL_CONTENT = 206
}

/** How a background transfer ended. A [Finished] response's body, if it was kept, is already in the part file. */
internal sealed interface TransferOutcome {
  /** The server answered with [status] (or [BackgroundDownloadSession.STATUS_NOT_SAVED]). */
  data class Finished(val status: Int, val totalBytes: Long) : TransferOutcome

  /** The request didn't complete: the connection failed, or the task was cancelled. */
  data class Failed(val message: String, val cancelled: Boolean) : TransferOutcome
}

/** The part file a task fills, relative to the downloads directory, and the offset its request starts at. */
internal data class TransferTag(val relativePath: String, val offset: Long) {
  fun encode(): String = "$offset|$relativePath"

  companion object {
    fun decode(description: String?): TransferTag? {
      val offset = description?.substringBefore('|', "")?.toLongOrNull() ?: return null
      return TransferTag(description.substringAfter('|'), offset)
    }
  }
}
