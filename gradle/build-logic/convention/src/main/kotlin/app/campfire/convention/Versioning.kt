// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.convention

import org.gradle.api.Project

/** Fallback versionCode for local/dev builds where no version information is provided. */
const val DEV_VERSION_CODE = 999999999

/**
 * Resolves the effective versionCode for a build:
 * 1. An explicit `CAMPFIRE_VERSIONCODE` gradle property (CI alpha/PR builds pass the run number)
 * 2. The committed `campfire.versionCode` from gradle.properties — the source of truth, kept in
 *    lockstep with `campfire.version` by scripts/release + a CI guard. Reading it directly (rather
 *    than deriving) means the build produces the exact same code F-Droid's UpdateCheckData reads,
 *    with no risk of drift, and `git checkout <tag> && ./gradlew ...` is reproducible with no args.
 * 3. [DEV_VERSION_CODE] as a last-resort safety net (only if the property is missing/unparseable).
 */
fun Project.campfireVersionCode(): Int =
  providers.gradleProperty("CAMPFIRE_VERSIONCODE").orNull?.toIntOrNull()
    ?: providers.gradleProperty("campfire.versionCode").orNull?.toIntOrNull()
    ?: DEV_VERSION_CODE

fun Project.campfireVersionName(): String =
  providers.gradleProperty("CAMPFIRE_VERSIONNAME").orNull
    ?: providers.gradleProperty("campfire.version").get()

/**
 * Derives a deterministic versionCode from a semver version name so a given release tag
 * always maps to the same code: MMmmppRR where RR is the rc number, or 99 for a final
 * release. Ordering holds: 1.0.0-rc3 = 1000003 < 1.0.0 = 1000099 < 1.0.1-rc1 = 1000101.
 * Returns null for anything that isn't `[v]major.minor.patch[-rcN]` (e.g. `-alpha` tags).
 */
fun deriveVersionCode(versionName: String): Int? {
  val match = SEMVER_REGEX.find(versionName) ?: return null
  val (major, minor, patch, rc) = match.destructured
  return major.toInt() * 1_000_000 +
    minor.toInt() * 10_000 +
    patch.toInt() * 100 +
    (rc.toIntOrNull() ?: 99)
}

private val SEMVER_REGEX = """^v?(\d+)\.(\d+)\.(\d+)(?:-rc(\d+))?$""".toRegex(RegexOption.IGNORE_CASE)
