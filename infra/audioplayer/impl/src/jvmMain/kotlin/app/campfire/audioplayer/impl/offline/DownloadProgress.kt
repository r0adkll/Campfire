// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.offline

import app.campfire.audioplayer.offline.OfflineDownload

/** The runtime state of one file of a desktop download. */
internal data class FileProgress(
  val status: Status,
  val bytes: Long = 0L,
  /** Total bytes as reported by the server, or <= 0 when not yet known. */
  val totalBytes: Long = -1L,
  val updatedAtMs: Long = -1L,
) {
  enum class Status { Queued, Downloading, Completed, Failed }
}

/**
 * Condenses a download's per-file progress into the item-level [OfflineDownload] the UI renders.
 * Mirrors the Android tracker's precedence: any failed file fails the download, then any file in
 * flight makes it downloading, then queued; it's complete only when every file is.
 */
internal fun DownloadManifest.toOfflineDownload(files: Map<Int, FileProgress>): OfflineDownload {
  val progress = this.files.map { file ->
    val state = files[file.trackIndex] ?: FileProgress(FileProgress.Status.Queued)
    val total = state.totalBytes.takeIf { it > 0 } ?: file.expectedBytes
    state to total
  }
  val statuses = progress.map { (state, _) -> state.status }.toSet()

  val state = when {
    statuses.isEmpty() -> OfflineDownload.State.None
    FileProgress.Status.Failed in statuses -> OfflineDownload.State.Failed
    FileProgress.Status.Downloading in statuses -> OfflineDownload.State.Downloading
    FileProgress.Status.Queued in statuses -> OfflineDownload.State.Queued
    else -> OfflineDownload.State.Completed
  }

  val bytes = progress.sumOf { (state, _) -> state.bytes }
  val contentLength = if (progress.all { (_, total) -> total > 0 }) progress.sumOf { (_, total) -> total } else -1L
  val percent = when {
    state == OfflineDownload.State.Completed -> 1f
    contentLength > 0 -> (bytes.toDouble() / contentLength).toFloat().coerceIn(0f, 1f)
    else -> 0f
  }

  return OfflineDownload(
    libraryItemId = libraryItemId,
    episodeId = episodeId,
    state = state,
    startTimeMs = createdAtMs,
    updateTimeMs = maxOf(createdAtMs, progress.maxOfOrNull { (state, _) -> state.updatedAtMs } ?: -1L),
    contentLength = contentLength,
    progress = OfflineDownload.Progress(
      bytes = bytes,
      percent = percent,
      indeterminate = state != OfflineDownload.State.Completed && contentLength <= 0,
    ),
  )
}
