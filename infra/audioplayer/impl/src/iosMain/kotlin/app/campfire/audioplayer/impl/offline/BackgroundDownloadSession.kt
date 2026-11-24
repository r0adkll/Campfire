// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.offline

import app.campfire.core.logging.Cork
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.seconds
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import platform.Foundation.NSData
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
import platform.Foundation.NSURLSessionDownloadTaskResumeData
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
 * resumes from. A file that lands while nothing waits for it, as after a relaunch or when it was
 * started ahead of its turn, still ends up in its part file, where the queue finds it. A transfer
 * that ends early with resume data (stopped, cut off, or cancelled because the user quit the app)
 * saves it beside its part file ([SavedResumeData]), so the next attempt carries on from there. All
 * of the session's bookkeeping runs on its serial delegate queue.
 */
@OptIn(ExperimentalForeignApi::class)
object BackgroundDownloadSession : Cork {

  override val tag: String = "BackgroundDownloads"
  override val enabled: Boolean = true

  private const val IDENTIFIER = "app.campfire.downloads"

  /** A response whose body couldn't be saved to its part file. */
  internal const val STATUS_NOT_SAVED = -1

  /** How long stopping a transfer waits for iOS to hand over its resume data. */
  private val STOP_TIMEOUT = 5.seconds

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

  /** This launch's unfinished tasks, whether or not anything waits for them. */
  private val started = mutableMapOf<ULong, NSURLSessionDownloadTask>()

  /** Tasks cancelled for good, whose resume data isn't worth keeping. */
  private val discarded = mutableSetOf<ULong>()
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

  /** Every unfinished task, from this launch or one before it, with its tag. */
  internal suspend fun unfinishedTasks(): List<Pair<NSURLSessionDownloadTask, TransferTag>> {
    val tasks = suspendCancellableCoroutine { continuation ->
      session.getAllTasksWithCompletionHandler { tasks -> continuation.resume(tasks.orEmpty()) }
    }
    return tasks.filterIsInstance<NSURLSessionDownloadTask>().mapNotNull { task ->
      val tag = TransferTag.decode(task.taskDescription)
      val unfinished = task.state == NSURLSessionTaskStateRunning || task.state == NSURLSessionTaskStateSuspended
      if (tag != null && unfinished) task to tag else null
    }
  }

  /** The unfinished task filling [relativePath], from this launch or one before it. */
  internal suspend fun runningTask(relativePath: String): Pair<NSURLSessionDownloadTask, TransferTag>? =
    unfinishedTasks().firstOrNull { (_, tag) -> tag.relativePath == relativePath }

  internal fun newTask(request: NSURLRequest, tag: TransferTag): NSURLSessionDownloadTask =
    session.downloadTaskWithRequest(request).apply { taskDescription = tag.encode() }

  /** A task carrying on an interrupted transfer, tagged like the one it continues. */
  internal fun resumedTask(resumeData: NSData, tag: TransferTag): NSURLSessionDownloadTask =
    session.downloadTaskWithResumeData(resumeData).apply { taskDescription = tag.encode() }

  /** Runs [task] without waiting for it. Its outcome is kept for whoever later [await]s it. */
  internal fun start(task: NSURLSessionDownloadTask) {
    delegateQueue.addOperationWithBlock {
      started[task.taskIdentifier] = task
      task.resume()
    }
  }

  /**
   * Runs [task] (if it isn't already) and waits for it to finish. Cancelling the wait stops the
   * task, keeping its resume data. [onProgress] gets (bytes in the part file, total bytes or <= 0
   * when unknown).
   */
  internal suspend fun await(task: NSURLSessionDownloadTask, onProgress: (Long, Long) -> Unit): TransferOutcome {
    val id = task.taskIdentifier
    val transfer = Transfer(task, onProgress)
    delegateQueue.addOperationWithBlock {
      val finished = unclaimed.remove(id)
      if (finished != null) {
        transfer.outcome.complete(finished)
      } else {
        transfers[id] = transfer
        started[id] = task
        task.resume()
      }
    }
    return try {
      transfer.outcome.await()
    } catch (e: CancellationException) {
      // The resume data is saved before the outcome completes, so it's on disk once this returns
      task.cancelByProducingResumeData { }
      withContext(NonCancellable) { withTimeoutOrNull(STOP_TIMEOUT) { transfer.outcome.await() } }
      delegateQueue.addOperationWithBlock { transfers.remove(id) }
      throw e
    }
  }

  /** Stops every unfinished transfer into [directory] (relative to the downloads directory), keeping their resume data. */
  internal suspend fun stopAll(directory: String) {
    unfinishedTasks()
      .filter { (_, tag) -> tag.relativePath.startsWith("$directory/") }
      .forEach { (task, _) -> task.cancelByProducingResumeData { } }
  }

  /**
   * Cancels every unfinished transfer into [directory] for good. No resume data is kept, so iOS
   * deletes the bytes they'd downloaded.
   */
  internal suspend fun discardAll(directory: String) {
    val tasks = unfinishedTasks().filter { (_, tag) -> tag.relativePath.startsWith("$directory/") }
    onDelegateQueue {
      for ((task, _) in tasks) {
        discarded += task.taskIdentifier
        task.cancel()
      }
    }
  }

  /** Gives up on [resumeData]: cancelling a task made from it lets iOS delete the bytes it kept. */
  internal suspend fun discardResumeData(resumeData: NSData) {
    val task = session.downloadTaskWithResumeData(resumeData)
    onDelegateQueue {
      discarded += task.taskIdentifier
      task.cancel()
    }
  }

  private suspend fun onDelegateQueue(block: () -> Unit) = suspendCancellableCoroutine { continuation ->
    delegateQueue.addOperationWithBlock {
      block()
      continuation.resume(Unit)
    }
  }

  private fun onWrite(task: NSURLSessionDownloadTask, written: Long, expected: Long) {
    val transfer = transfers[task.taskIdentifier] ?: return
    val tag = TransferTag.decode(task.taskDescription) ?: return
    // A server that ignores the range sends the whole file from the start
    val base = if (task.httpStatus() == HTTP_PARTIAL_CONTENT) tag.offset else 0L
    transfer.onProgress(base + written, if (expected > 0) base + expected else -1L)
  }

  /**
   * Saves the body into its part file. iOS deletes [location] as soon as this returns. A resumed
   * transfer hands over the whole body from its tag's offset, like the one it continues.
   */
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
    started.remove(id)
    val keepResumeData = !discarded.remove(id)
    val status = savedStatus.remove(id) ?: task.httpStatus()
    val cancelled = error?.domain == NSURLErrorDomain && error?.code == NSURLErrorCancelled
    val resumeData = error?.userInfo?.get(NSURLSessionDownloadTaskResumeData) as? NSData
    if (resumeData != null && keepResumeData) saveResumeData(task, resumeData)
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

  private fun saveResumeData(task: NSURLSessionTask, resumeData: NSData) {
    val tag = TransferTag.decode(task.taskDescription) ?: return
    try {
      SavedResumeData.save(Path(iosDownloadsRoot(), tag.relativePath), tag.offset, resumeData)
      ibark { "Saved resume data for ${tag.relativePath}" }
    } catch (e: Exception) {
      ebark(e) { "Unable to save resume data for ${tag.relativePath}" }
    }
  }

  /**
   * A finished transfer that nothing waited for can find another one this launch started for the
   * same part file: after a relaunch, the queue can resume a file before iOS has delivered the
   * earlier launch's transfer that already finished it. The finished one stands in for the others,
   * which are cancelled rather than download the file again; returns the one something waits for.
   */
  private fun supersededTransfer(task: NSURLSessionTask, outcome: TransferOutcome): Transfer? {
    if (outcome !is TransferOutcome.Finished) return null
    if (outcome.status != HTTP_OK && outcome.status != HTTP_PARTIAL_CONTENT) return null
    val relativePath = TransferTag.decode(task.taskDescription)?.relativePath ?: return null
    val duplicates = started.values.filter { TransferTag.decode(it.taskDescription)?.relativePath == relativePath }
    if (duplicates.isEmpty()) return null
    ibark { "A finished transfer already filled $relativePath, cancelling ${duplicates.size} other(s) for it" }
    var waiting: Transfer? = null
    for (duplicate in duplicates) {
      val id = duplicate.taskIdentifier
      started.remove(id)
      discarded += id
      duplicate.cancel()
      transfers.remove(id)?.let { waiting = it }
    }
    return waiting
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
