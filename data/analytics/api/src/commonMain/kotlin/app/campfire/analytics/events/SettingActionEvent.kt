// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.analytics.events

@Suppress("FunctionName")
fun SettingActionEvent(
  obj: String,
  verb: Verb,
  noun: Any? = null,
) = ActionEvent(
  obj = obj,
  verb = verb,
  noun = noun,
  extras = mapOf(
    "source" to "settings",
  ),
)
