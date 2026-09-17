// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.whatsnew

import app.campfire.core.Platform
import app.campfire.whatsnew.api.ChangeSet
import app.campfire.whatsnew.api.VersionChanges
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlinx.serialization.json.Json

class ChangelogEntriesTest {

  private val entries: List<VersionEntry> = Json.decodeFromString(
    """
    [
      {"version":"1.1.1","date":null,"changes":[
        {"name":"Added","changes":[
          {"text":"Offline downloads","platforms":["DESKTOP"]},
          {"text":"Pull to refresh","platforms":["ANDROID","DESKTOP","IOS"]}
        ]},
        {"name":"Fixed","changes":[
          {"text":"Cast crash","platforms":["ANDROID"]}
        ]},
        {"name":"Removed","changes":[]}
      ]},
      {"version":"1.1.0","date":"2026-07-01","changes":[
        {"name":"Added","changes":[
          {"text":"Equalizer","platforms":["ANDROID"]}
        ]}
      ]}
    ]
    """,
  )

  @Test
  fun `keeps only the entries for the platform`() {
    assertThat(entries.forPlatform(Platform.ANDROID)).isEqualTo(
      listOf(
        VersionChanges(
          version = "1.1.1",
          date = null,
          changes = listOf(
            ChangeSet("Added", listOf("Pull to refresh")),
            ChangeSet("Fixed", listOf("Cast crash")),
          ),
        ),
        VersionChanges("1.1.0", "2026-07-01", listOf(ChangeSet("Added", listOf("Equalizer")))),
      ),
    )
  }

  @Test
  fun `drops versions with nothing for the platform`() {
    assertThat(entries.forPlatform(Platform.DESKTOP)).isEqualTo(
      listOf(
        VersionChanges(
          version = "1.1.1",
          date = null,
          changes = listOf(ChangeSet("Added", listOf("Offline downloads", "Pull to refresh"))),
        ),
      ),
    )
  }

  @Test
  fun `announces a version only when it has changes for the platform`() {
    assertThat(entries.hasChangesFor("1.1.0", Platform.ANDROID)).isTrue()
    assertThat(entries.hasChangesFor("1.1.0", Platform.DESKTOP)).isFalse()
  }

  @Test
  fun `announces a version without its own section`() {
    assertThat(entries.hasChangesFor("1.2.0-alpha01", Platform.IOS)).isTrue()
  }
}
