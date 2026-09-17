// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.convention.changelog

/**
 * The platforms a CHANGELOG.md entry can be tagged with (`- [Desktop] …`). Constant names are
 * the wire keys in `changelog.json` and must match `app.campfire.core.Platform`.
 *
 * An untagged entry applies to every platform that shipped in its release: the `Unreleased`
 * section, plus every version from [firstRelease] onward. Set [firstRelease] to the version a
 * platform first ships in when cutting that release; `null` means it hasn't shipped yet.
 */
enum class ChangelogPlatform(
  val tag: String,
  val firstRelease: String?,
) {
  ANDROID("Android", "0.1.0-alpha"),
  DESKTOP("Desktop", "1.2.0"),
  IOS("iOS", null),
  ;

  companion object {
    fun fromTag(tag: String): ChangelogPlatform? = entries.firstOrNull { it.tag.equals(tag, ignoreCase = true) }
  }
}
