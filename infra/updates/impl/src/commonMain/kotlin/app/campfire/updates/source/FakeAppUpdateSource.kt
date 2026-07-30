// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.updates.source

import app.campfire.settings.api.DevSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject

/**
 * A fake [AppUpdateSource] for debug builds, driven entirely by the fake app update
 * toggles in [DevSettings] (surfaced in the Developer settings pane) so the update
 * widget, sign-in prompt, and update sheet can be tested without a real
 * Firebase App Distribution release.
 *
 * This is not contributed to the DI graph directly; debug-only platform bindings
 * delegate to it.
 */
@Inject
class FakeAppUpdateSource(
  private val devSettings: DevSettings,
) : AppUpdateSource {

  override val isSupported: Boolean = true

  override fun changes(): Flow<Unit> = combine(
    devSettings.observeFakeAppUpdateSignedIn(),
    devSettings.observeFakeAppUpdateAvailable(),
  ) { _, _ -> }
    .drop(1)
    .map { }

  override fun isSignedIn(): Boolean = devSettings.fakeAppUpdateSignedIn

  override suspend fun signIn() {
    delay(SIGN_IN_DELAY_MS)
    devSettings.fakeAppUpdateSignedIn = true
  }

  override suspend fun isUpdateAvailable(): Boolean = devSettings.fakeAppUpdateAvailable

  override suspend fun getAvailableUpdate(): AppUpdate? {
    return if (devSettings.fakeAppUpdateAvailable) FAKE_UPDATE else null
  }

  override suspend fun installUpdate(): Flow<AppUpdateProgress> = flow {
    emit(AppUpdateProgress(-1L, -1L, AppUpdateProgress.Status.Pending))
    delay(PENDING_DELAY_MS)

    repeat(DOWNLOAD_STEPS) { step ->
      delay(DOWNLOAD_STEP_DELAY_MS)

      if (devSettings.fakeAppUpdateFailDownload && step == DOWNLOAD_STEPS / 2) {
        emit(
          AppUpdateProgress(
            bytes = TOTAL_BYTES * step / DOWNLOAD_STEPS,
            totalBytes = TOTAL_BYTES,
            status = AppUpdateProgress.Status.Failed,
          ),
        )
        return@flow
      }

      emit(
        AppUpdateProgress(
          bytes = TOTAL_BYTES * (step + 1) / DOWNLOAD_STEPS,
          totalBytes = TOTAL_BYTES,
          status = AppUpdateProgress.Status.Downloading,
        ),
      )
    }

    emit(AppUpdateProgress(TOTAL_BYTES, TOTAL_BYTES, AppUpdateProgress.Status.Downloaded))
  }

  companion object {
    private val FAKE_UPDATE = AppUpdate(
      versionName = "v9.9.9-fake",
      versionCode = 999_999_999L,
      releaseNotes = """
        This is a faked release for testing the app update flow.

        ## What's Changed
        * Added a fake feature that does absolutely nothing
        * Fixed a bug that never existed
        * Improved the performance of imaginary code paths
        * Updated dependencies to versions that aren't real
      """.trimIndent(),
    )

    private const val SIGN_IN_DELAY_MS = 1_000L
    private const val PENDING_DELAY_MS = 1_000L
    private const val DOWNLOAD_STEPS = 20
    private const val DOWNLOAD_STEP_DELAY_MS = 200L
    private const val TOTAL_BYTES = 48_000_000L
  }
}
