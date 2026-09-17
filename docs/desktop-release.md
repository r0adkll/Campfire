# Desktop releases

Desktop packages are built by [Hydraulic Conveyor](https://conveyor.hydraulic.dev), configured in
[`conveyor.conf`](../conveyor.conf) at the repo root. One run on any machine produces, signs and
publishes all four packages:

| Machine | Package |
|---|---|
| `mac.aarch64`, `mac.amd64` | signed + notarized macOS app |
| `windows.amd64` | MSIX installer |
| `linux.amd64.glibc` | `.deb` and tarball |

Everything the packages contain — classpath, JDK, version, main class, and the per-machine skiko
and FFmpeg natives — comes from Gradle via `:app:desktop:printConveyorConfig`, so dependencies are
declared in exactly one place. Conveyor is free for OSI-licensed projects, which `app.vcs-url` in
`conveyor.conf` is what claims.

## Icons

Two variants of the same artwork, both under `app/desktop/src/main/resources/`, and both used by
the running app as well as by packaging:

- `icon.png` — full-bleed square, for Windows, Linux and the window icon.
- `icon-macos.png` — inset inside Apple's rounded square with a shadow, for the macOS bundle and
  the Dock. macOS icons are deliberately not edge-to-edge; one that fills its canvas reads as
  noticeably larger than every icon beside it.

Regenerate with `tools/desktop-icon/generate.sh` after changing the artwork. The macOS silhouette
is committed as an SVG traced from a stock system icon, so the script needs only ImageMagick and
librsvg.

## One-time setup

Steps 3–5 are the ones that produce secrets; none of them can be automated.

### 1. Install the CLI

```bash
brew install --cask hydraulic-conveyor
```

### 2. Claim the open-source licence

Sign up at [hydraulic.dev](https://hydraulic.dev) and follow their open-source instructions.
Campfire is GPL-3.0-only, an OSI-approved licence, so there is nothing to pay.

### 3. Generate the root signing key

```bash
conveyor keys generate
```

Everything else — the macOS identity, the Windows identity, the Sparkle key that signs updates —
is derived from this one key, so **it is the secret that matters**. Back it up: lose it and you
cannot ship an update existing installs will accept.

On macOS it does *not* land in a file. It goes into the login Keychain under the service
`Hydraulic Conveyor Root Key`, and the `defaults.conf` it writes to
`~/Library/Preferences/Hydraulic/Conveyor/` holds only the pointer `app.signing-key = "keyring"`.
(`conveyor keys generate --no-keyring` would put the key in the file instead, but there is no
reason to: the Keychain is the safer place, and it is a one-line difference for CI.)

CI has neither a Keychain nor a `defaults.conf` — that file is gitignored, being secret — so the
key has to be copied into a repository secret once. Pipe it straight from the Keychain into the
secret so it never lands on screen, on the clipboard or on disk:

```bash
security find-generic-password -s "Hydraulic Conveyor Root Key" -w \
  | tr -d '\n' \
  | gh secret set CONVEYOR_SIGNING_KEY --repo r0adkll/Campfire
```

macOS shows a Keychain authorisation prompt the first time — the item's ACL grants access to the
`conveyor` binary, not to `security`, so approve it (Always Allow, if you would rather not see it
again). `conveyor json` is not an alternative route: it redacts `app.signing-key`. The `tr` matters
too — the secret must not carry the trailing newline `security` prints.

The other three secrets can be lifted straight out of the same `defaults.conf`, which avoids
retyping an app-specific password by hand:

```bash
conf=~/Library/Preferences/Hydraulic/Conveyor/defaults.conf
for pair in team-id:APPLE_TEAM_ID apple-id:APPLE_ID app-specific-password:APPLE_APP_SPECIFIC_PASSWORD; do
  key=${pair%%:*}; secret=${pair##*:}
  sed -n "s/^[[:space:]]*$key[[:space:]]*=[[:space:]]*//p" "$conf" | tr -d '\"\n' \
    | gh secret set "$secret" --repo r0adkll/Campfire
done
```

The release workflow no-ops entirely until that secret exists.

### 4. Apple Developer ID certificate

1. In the Apple Developer console, request a **Developer ID Application** certificate.
2. Upload the `apple.csr` that step 3 wrote to `~/Library/Preferences/Hydraulic/Conveyor/`.
3. Download the `.cer` Apple returns — it comes back immediately, with no review.
4. Commit it to `app/signing/apple.cer`. `conveyor.conf` already points `app.mac.certificate`
   there.

The `.cer` is a *public* certificate. Committing it is intentional and is what lets CI sign without
any extra secret.

Until this is done Conveyor self-signs, and macOS shows the unidentified-developer warning.

### 5. Notarization credentials

Notarization is what stops Gatekeeper from blocking the app on first launch.

`conveyor keys generate` leaves a stub for it in the Hydraulic `defaults.conf`, beside the key
pointer — with the values literally set to `TODO`. Fill all three in or Apple rejects the upload
with *"the supplied credentials aren't valid"*, which reads like a permissions problem and is not:

| Key | Value |
|---|---|
| `team-id` | your 10-character Team ID. `codesign -dv output/Campfire.app` prints it as `TeamIdentifier` once anything is signed |
| `apple-id` | the email address of the Apple Developer account |
| `app-specific-password` | generated at [account.apple.com](https://account.apple.com) → Sign-In and Security → App-Specific Passwords, in `xxxx-xxxx-xxxx-xxxx` form. Not your Apple ID password |

Signing and notarization fail independently: a build can carry a perfectly valid Developer ID
signature and still fail to notarize, so check both.

CI needs the same three as repository secrets:

```bash
gh secret set APPLE_TEAM_ID               --repo r0adkll/Campfire
gh secret set APPLE_ID                    --repo r0adkll/Campfire
gh secret set APPLE_APP_SPECIFIC_PASSWORD --repo r0adkll/Campfire
```

Their values are the ones already in `~/Library/Preferences/Hydraulic/Conveyor/defaults.conf`.
The app-specific password is generated at [account.apple.com](https://account.apple.com) under
Sign-In and Security → App-Specific Passwords; it is not your Apple ID password.

`conveyor.conf` reads all three as *optional* substitutions, which is what lets one file serve both
cases: unset locally, the keys stay undefined and the values from `defaults.conf` come through
untouched; set in CI, they supply what no `defaults.conf` is there to provide. A build with none of
them available still works — it just produces signed-but-unnotarized output, which is fine for
testing.

### Windows: a caveat, not a step

Windows packages are MSIX, and Windows will not install an MSIX signed by an untrusted CA. Without
an Authenticode certificate (roughly $200–400/yr) or Azure Trusted Signing, Windows users must
install the self-signed certificate first; Conveyor generates those instructions on the download
page. Wiring a real certificate up later is a one-line change beside the macOS one.

## Building locally

```bash
./gradlew :app:desktop:proguardReleaseJars     # the shrunk jars conveyor.conf ships
conveyor make site                             # all four packages, into output/
```

Two mac machines are configured, so any single-machine task has to say which one — plain
`conveyor make mac-app` fails with "Task mac-app is ambiguous":

```bash
conveyor -Kapp.machines=mac.aarch64 make mac-app             # fast, signs but does not notarize
conveyor -Kapp.machines=mac.aarch64 make notarized-mac-zip   # round-trips through Apple
```

The first run adds a `conveyor.compatibility-level` line to `conveyor.conf`. Commit it.

Check the signature rather than assuming it. Note that `mac-app` deliberately skips notarization,
so `spctl` reporting *Unnotarized Developer ID* there is the expected result, not a failure — it is
`notarized-mac-zip` and `site` that go to Apple:

```bash
codesign -dv --verbose=2 output/Campfire.app        # want: Authority=Developer ID Application: ...
codesign --verify --deep --strict output/Campfire.app
spctl -a -vvv -t install output/Campfire.app        # want: accepted, source=Notarized Developer ID
xcrun stapler validate output/Campfire.app
```

`app.inputs` drops the build host's skiko and FFmpeg natives from the ProGuard output, leaving the
per-machine inputs to supply the right ones. Confirm a package carries exactly one platform's
natives, and that they are its own — an arm64 Mac build should list only `macos-arm64` alongside
the two content-hashed platform-neutral jars:

```bash
find output -name '*.jar' | sed 's|.*/||' | grep -E 'skiko-awt-runtime|^ffmpeg-|^javacpp-'
```

## Releasing

The `desktop` job in [`.github/workflows/release.yml`](../.github/workflows/release.yml) runs on
every published GitHub release, alongside the Android jobs. It runs `conveyor make site`, then
uploads every file in `output/` (the packages *and* the update metadata) to that release with
`gh release upload`. Clients poll `releases/latest/download`, derived from `app.vcs-url`, so
updates need no other hosting.

It deliberately does not use `conveyor make copied-site`. Conveyor's GitHub upload always creates
the release itself and fails with "There is already a GitHub Release … for version X" when one
exists, and `scripts/release` creates that release first — it is what triggers the workflow.

The version is pinned to the release tag with `-Pcampfire.version=<tag>`, deliberately not with
`CAMPFIRE_VERSIONNAME` — the Android release-signing guard keys off that variable and would fail
the desktop job for want of a keystore it never needs.

## Size and native libraries

`app.jvm.extract-native-libraries` is on, via the vendored
[`conveyor/extract-native-libraries.conf`](../conveyor/extract-native-libraries.conf). It moves
native libraries out of their JARs into the JVM lib folder, signs them, and deletes the ones built
for other platforms. Two things depend on it, and the first is not optional:

1. **Notarization.** Apple's notary service walks into JARs and demands a Developer ID signature on
   every Mach-O binary it finds, run or not. Left inside their JARs those binaries keep the
   linker's ad-hoc signature and Apple rejects the submission.
2. **Size.** `sqlite-jdbc` alone ships 25.7 MB of natives for platforms no single package can load
   — Android, FreeBSD and Musl among them. After extraction its JAR holds empty directories.

Conveyor decides what is a native library **by file extension** — `.dylib`, `.so`, `.dll`. Two
things in this app's dependency graph fall outside that, and both had to be handled:

- **JNA** ships its macOS binary as `libjnidispatch.jnilib`. Extraction handles it; in-JAR signing
  does not, which is what made extraction mandatory rather than merely nice.
- **bytedeco FFmpeg** ships `ffmpeg` and `ffprobe`, extension-less executables that nothing
  recognises. `:app:desktop` repacks that archive without them — see `ffmpegNativesWithoutCliTools`
  in its build script. Nothing in the app runs them; the audio engine uses libav* through JNI.

Adding a dependency that carries natives under some other naming convention will fail notarization
the same way, and the error names the exact file.

### ProGuard is not in the shipping path

`buildTypes.release.proguard` shrinks the jars for Compose's own packaging (about 18%, 107.5 MB to
87.8 MB) and CI builds that variant to keep the rules honest, but **Conveyor does not use its
output**, and pointing it there would be a mistake:

- JARs handed to Conveyor through a hand-written `app.inputs` entry bypass its native-library
  handling entirely. They are neither signed nor pruned — which silently reintroduces both problems
  above.
- Listing the shrunk JARs individually instead overflows Conveyor's 16 KB argument buffer, all 264
  of them.

So the shipped classpath comes from the Conveyor Gradle plugin, and the size win comes from
extraction rather than from shrinking.

## Troubleshooting

**Natives fail to load** (skiko, FFmpeg, SQLite, JNA). Extraction moves them into the JVM lib
folder, so a library that insists on unpacking its own copy from a JAR finds nothing there. The fix
is a system property pointing it at `<libpath>`, in
`conveyor/extract-native-libraries.conf` — skiko and JavaCPP already have theirs, below the
upstream block. If a native loads but is refused at the hardened runtime, try
`app.mac.entitlements-plist."com.apple.security.cs.disable-library-validation" = true`.

**`NoClassDefFoundError` or a TLS handshake failure in a packaged build, but not under
`:app:desktop:run`.** Conveyor uses `jlink` to strip the bundled JDK to the modules `jdeps` can
see, and `jdeps` only sees bytecode references — anything reached reflectively or through
`ServiceLoader` is invisible to it. Add the module to `app.jvm.modules` in `conveyor.conf`;
`jdk.crypto.ec` is already there because losing it breaks every HTTPS call to the server while
leaving the app otherwise healthy.

**A package is missing skiko or has no audio.** Its machine is listed in `app.machines` but has no
natives declared in `:app:desktop`. The two lists have to move together — see
`machineDependencies()` in `app/desktop/build.gradle.kts`.

**Notarization is rejected over one file.** The message names it. If it is a Mach-O binary
inside a JAR, Conveyor did not recognise it as a native — see *Size and native libraries* above for
the two cases already handled and how.

**Something works under `:app:desktop:run` but not in a package.** `conveyor -Kapp.machines=mac.aarch64
make mac-app` builds in well under a minute and skips the Apple round-trip, so iterate on that
before running a full notarization.
