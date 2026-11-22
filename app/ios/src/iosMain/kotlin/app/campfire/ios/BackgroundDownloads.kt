// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ios

import app.campfire.audioplayer.impl.offline.BackgroundDownloadSession

/**
 * Called from the app delegate (`iOSApp.swift`) when iOS relaunches or wakes the app to deliver
 * downloads that finished in the background. Returns false for a session this app doesn't own.
 */
fun handleEventsForBackgroundURLSession(identifier: String, completionHandler: () -> Unit): Boolean =
  BackgroundDownloadSession.handleEventsForBackgroundURLSession(identifier, completionHandler)
