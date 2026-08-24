// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.convention

import java.security.MessageDigest
import java.util.zip.Adler32
import org.gradle.api.Project

/**
 * F-Droid does a byte-for-byte reproducible build of the foss flavor. R8 embeds a `pg-map-id`
 * (a hash of the ProGuard/R8 mapping) in the DEX marker that varies by build environment even when
 * the bytecode is identical — the sole thing that broke reproducibility. The mapping isn't published
 * for the foss build, so the id is cosmetic: normalize it to a fixed value right after R8 and before
 * packaging, so both our CI and F-Droid's rebuild produce identical DEX (and, since baseline.prof is
 * compiled afterwards from this DEX, an identical profile too). standard/alpha are untouched.
 */
fun Project.normalizeFossReleasePgMapId() {
  tasks.matching { it.name == "minifyFossReleaseWithR8" }.configureEach {
    val dexDir = layout.buildDirectory
      .dir("intermediates/dex/fossRelease/minifyFossReleaseWithR8").get().asFile
    val id = "0".repeat(64).toByteArray(Charsets.ISO_8859_1)
    doLast {
      val key = "\"pg-map-id\":\"".toByteArray(Charsets.ISO_8859_1)
      dexDir.listFiles { f -> f.name.matches(Regex("classes\\d*\\.dex")) }?.forEach { dexFile ->
        val bytes = dexFile.readBytes()
        var at = -1
        run {
          outer@ for (i in 0..bytes.size - key.size) {
            for (j in key.indices) if (bytes[i + j] != key[j]) continue@outer
            at = i; return@run
          }
        }
        if (at < 0) return@forEach
        val start = at + key.size
        var end = start
        while (end < bytes.size && bytes[end] != '"'.code.toByte()) end++
        require(end - start == id.size) { "unexpected pg-map-id length ${end - start}" }
        id.copyInto(bytes, start)
        // recompute DEX SHA-1 signature (bytes[32..] -> bytes[12..32]) and Adler-32 (bytes[12..] -> bytes[8..12])
        MessageDigest.getInstance("SHA-1").digest(bytes.copyOfRange(32, bytes.size)).copyInto(bytes, 12)
        val adler = Adler32().apply { update(bytes, 12, bytes.size - 12) }.value
        for (i in 0 until 4) bytes[8 + i] = ((adler shr (8 * i)) and 0xffL).toByte()
        dexFile.writeBytes(bytes)
      }
    }
  }
}
