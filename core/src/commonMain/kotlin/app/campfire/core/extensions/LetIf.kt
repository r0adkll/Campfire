// Copyright 2023, Google LLC, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: Apache-2.0

package app.campfire.core.extensions

inline fun <T> T.fluentIf(condition: Boolean, block: T.() -> T): T {
  return if (condition) block() else this
}
