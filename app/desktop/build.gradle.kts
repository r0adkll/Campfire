// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

import app.campfire.convention.campfireVersionCode
import app.campfire.convention.campfireVersionName
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
  id("app.campfire.kotlin.jvm")
  id("app.campfire.compose")
  alias(libs.plugins.ksp)
  alias(libs.plugins.about.libraries)
  alias(libs.plugins.buildConfig)
  alias(libs.plugins.conveyor)
}

kotlin {
  compilerOptions {
    freeCompilerArgs.add("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")
  }
}

val desktopAudioEngine = providers.gradleProperty("campfire_desktop_audio_engine").orNull ?: "ffmpeg"

val versionName = campfireVersionName()

/**
 * Native package formats want a plain `major.minor.patch` — macOS rejects anything else outright.
 * Release candidates tag as `1.2.0-rc1`, so the suffix is dropped here while the app itself still
 * reports the full name through [ApplicationInfo].
 */
val packageVersionName = versionName.removePrefix("v").substringBefore('-')

// Conveyor reads app.version off the project; every package format it builds requires one.
version = packageVersionName

/**
 * Desktop has no build types, so `DEBUG` follows the environment: a developer's machine builds a
 * debug app, CI builds a release one, and `-Pcampfire.desktop.debug=<bool>` overrides either way.
 *
 * Getting this wrong is not cosmetic, and the hardcoded `debugBuild = true` this replaces got it
 * wrong for every build: it raises the Ktor log level and stops auth headers being redacted,
 * disables analytics, and forces the developer settings pane on.
 */
val isDebugBuild = providers.gradleProperty("campfire.desktop.debug").orNull?.toBoolean()
  ?: !providers.environmentVariable("CI").isPresent

buildConfig {
  packageName("app.campfire")
  buildConfigField("String", "VERSION_NAME", "\"$versionName\"")
  buildConfigField("int", "VERSION_CODE", "${campfireVersionCode()}")
  buildConfigField("boolean", "DEBUG", "$isDebugBuild")
  useKotlinOutput()
}

dependencies {
  implementation(projects.app.common)

  if (desktopAudioEngine == "vlc" || desktopAudioEngine == "both") {
    implementation(projects.infra.audioplayer.engineVlc)
  }
  if (desktopAudioEngine != "vlc") {
    implementation(projects.infra.audioplayer.engineFfmpeg)
  }

  implementation(libs.kimchi.annotations)
  implementation(libs.kotlininject.runtime)

  ksp(libs.kotlininject.ksp)
  ksp(libs.kimchi.compiler)
}

/**
 * Skiko and the FFmpeg natives are the only per-machine pieces of the desktop app, and Conveyor
 * packages every machine from one build, so they cannot be resolved against whatever host happens
 * to be building — `compose.desktop.currentOs` would weld this machine's skiko into the Windows
 * and Linux packages alike.
 *
 * The Conveyor plugin's machine configurations route each artifact to the package it belongs in,
 * and it wires `implementation` to extend the *host's* configuration, so `:app:desktop:run` keeps
 * working with nothing extra declared.
 *
 * The machine list matches Conveyor's default `app.machines`.
 */
machineDependencies("macAarch64", composeTarget = "macos-arm64", javacppPlatform = "macosx-arm64")
machineDependencies("macAmd64", composeTarget = "macos-x64", javacppPlatform = "macosx-x86_64")
machineDependencies("windowsAmd64", composeTarget = "windows-x64", javacppPlatform = "windows-x86_64")
machineDependencies("linuxAmd64", composeTarget = "linux-x64", javacppPlatform = "linux-x86_64")

/** Declares one machine's skiko-backed Compose runtime and the FFmpeg/JavaCPP natives beside it. */
fun machineDependencies(machine: String, composeTarget: String, javacppPlatform: String) {
  val composeVersion = libs.versions.compose.multiplatform.get()
  val ffmpegNatives = ffmpegNativesWithoutCliTools(javacppPlatform)
  dependencies {
    add(machine, "org.jetbrains.compose.desktop:desktop-jvm-$composeTarget:$composeVersion")
    add(machine, files(ffmpegNatives))
    add(machine, variantOf(libs.bytedeco.javacpp) { classifier(javacppPlatform) })
  }

  // printConveyorConfig only prints each machine's classpath, and nothing else in a build resolves
  // a machine other than the host's, so without this the other machines' natives jars are never
  // built and Conveyor fails on the first missing one.
  tasks.named("printConveyorConfig") { dependsOn(ffmpegNatives) }
}

/**
 * bytedeco's FFmpeg natives archive for one platform, repacked without the `ffmpeg` and `ffprobe`
 * command-line executables.
 *
 * Apple's notary service walks into JARs and requires every Mach-O binary it finds to carry a
 * Developer ID signature, whether or not the app ever runs it. Conveyor re-signs natives by file
 * extension — `.dylib`, `.so`, `.dll` — and these two have none, so they keep the ad-hoc signature
 * the linker gave them and Apple rejects the whole submission over them.
 *
 * Dropping them costs nothing: the audio engine talks to libav* through JNI, and the only reference
 * to the CLI binding anywhere in the repo is FilterChainLoudnessTest, which resolves the untouched
 * archive through engine-ffmpeg's own testRuntimeOnly rather than through here.
 */
fun ffmpegNativesWithoutCliTools(javacppPlatform: String): TaskProvider<Zip> {
  val ffmpeg = libs.bytedeco.ffmpeg.get()
  val coordinates =
    "${ffmpeg.module.group}:${ffmpeg.module.name}:${ffmpeg.versionConstraint.requiredVersion}:$javacppPlatform"
  val natives = configurations
    .detachedConfiguration(dependencies.create(coordinates))
    .apply { isTransitive = false }

  val suffix = javacppPlatform.split('-').joinToString("") { part ->
    part.replaceFirstChar { it.uppercase() }
  }
  return tasks.register<Zip>("ffmpegNativesWithoutCliTools$suffix") {
    from(zipTree(natives.elements.map { it.single().asFile }))
    exclude("**/ffmpeg", "**/ffmpeg.exe", "**/ffprobe", "**/ffprobe.exe")

    // A plain Zip rather than a Jar so the upstream MANIFEST.MF is repacked as-is instead of being
    // regenerated underneath JavaCPP.
    archiveFileName.set("ffmpeg-natives-$javacppPlatform.jar")
    destinationDirectory.set(layout.buildDirectory.dir("ffmpeg-natives"))
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
  }
}

compose.desktop {
  application {
    mainClass = "app.campfire.MainKt"

    /**
     * Shrink, but never rename. The app is GPL-3.0 with public source, so obfuscation protects
     * nothing while making every crash report unreadable; `optimize` stays off because its
     * inlining and devirtualisation are what tend to break reflective frameworks, and it saves
     * little next to plain dead-code removal.
     *
     * `joinOutputJars` must stay false. ProGuard runs against the host's runtime classpath, so an
     * uber jar would fuse this machine's skiko and FFmpeg natives into a single artifact that
     * Conveyor could no longer swap out per machine.
     */
    buildTypes.release.proguard {
      version.set(libs.versions.proguard.get())
      configurationFiles.from(project.file("desktop-proguard-rules.pro"))
      obfuscate.set(false)
      optimize.set(false)
      joinOutputJars.set(false)
    }

    nativeDistributions {
      targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)

      // jlink keeps only the JDK modules jdeps finds referenced in bytecode, and a release
      // build drops all the way to opening the database before dying on
      // NoClassDefFoundError: java/sql/DriverManager. All but jdk.crypto.ec come from
      // `:app:desktop:suggestRuntimeModules`; that one is added because the TLS providers are
      // found through java.security rather than any bytecode reference, so jdeps cannot see it
      // and losing it breaks every HTTPS call while leaving the app otherwise healthy.
      // Kept in step with app.jvm.modules in conveyor.conf.
      modules(
        "java.instrument",
        "java.management",
        "java.net.http",
        "java.prefs",
        "java.sql",
        "jdk.crypto.ec",
        "jdk.unsupported",
      )
      packageName = "Campfire"
      packageVersion = packageVersionName
      description = "An unofficial client for Audiobookshelf"
      vendor = "The Scavenger's Software"
      copyright = "© 2026 The Scavenger's Software and the Campfire project contributors"
      licenseFile = rootProject.file("LICENSE")

      // Pinned rather than derived from packageName so the bundle identity stays put if the
      // display name ever changes, and matches the rdns-name Conveyor packages under.
      macOS {
        bundleID = "app.campfire"

        // Follows the bundled FFmpeg natives, which declare minos 15.0 and will not load on
        // anything older. Kept in step with app.mac.info-plist.LSMinimumSystemVersion in
        // conveyor.conf.
        minimumSystemVersion = "15.0"
      }
    }
  }
}

aboutLibraries {
  export.prettyPrint = true
}
