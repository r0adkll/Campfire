package app.campfire.core.model

import kotlin.time.Duration

data class AudioFile(
  val index: Int,
  val ino: String,
//  val metadata: FileMetadata,
  val addedAt: Long,
  val updatedAt: Long,
  val trackNumFromMeta: Int? = null,
  val discNumFromMeta: Int? = null,
  val trackNumFromFilename: Int? = null,
  val discNumFromFilename: Int? = null,
  val manuallyVerified: Boolean,
  val invalid: Boolean? = null,
  val exclude: Boolean,
  val error: String? = null,
  val format: String,
  val duration: Duration,
  val bitRate: Int,
  val language: String? = null,
  val codec: String,
  val timeBase: String,
  val channels: Int,
  val channelLayout: String,
//  val chapters: List<Chapter>,
  val embeddedCoverArt: String? = null,
//  val metaTags: AudioMetaTags,
  val mimeType: String,
)
