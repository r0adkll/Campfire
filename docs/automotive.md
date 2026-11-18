# Android Automotive OS

`:app:automotive` is Campfire for cars with Google built-in. The car's media app draws the library
and the player from Campfire's media session (`AudioPlayerService`, shared with the phone app and
Android Auto). The car app has only two windows of its own, and the car opens them only while parked:

- `SignInActivity`, opened from the "Sign in" button the car shows while nobody is signed in.
- `SettingsActivity`, opened from the car's settings button (`ACTION_APPLICATION_PREFERENCES`).

It has the same package as the phone app (`app.campfire.android`), so Play treats it as the same app
on another form factor. Play wants a separate bundle for that form factor, on that form factor's own
tracks, with a versionCode no phone release will reach: the phone's plus 1,000,000,000
(`campfireAutomotiveVersionCode()` in `gradle/build-logic`, mirrored in `fastlane/Fastfile`).

## Releases

The release workflow builds the car bundle beside the phone's and attaches it to the GitHub release
as `campfire-automotive-release.aab`. The `automotive-play-upload` job publishes it with
`fastlane android beta|production form_factor:automotive`. It goes to `automotive:beta` for rc tags
and `automotive:production` for final releases, with the phone release's notes.

That job only runs once the `PLAY_AUTOMOTIVE_ENABLED` repository variable is `true`, because Play
rejects uploads to an automotive track until the form factor is set up. One-time setup:

1. Play Console → **Advanced settings** → **Form factors** → **Add form factor** → **Android
   Automotive OS**.
2. Choose the **Android Automotive OS only** track type. The Console defaults to the phone's
   tracks, which Play doesn't allow for media apps, and the workflow uploads to `automotive:*`.
3. Add AAOS screenshots to the store listing (and to any custom listings): at least two portrait
   (800×1280) and two landscape (1024×768), taken on a generic AAOS emulator image with nothing
   that names a car maker. The emulator's *Automotive (1024p landscape)* and *Automotive Portrait*
   hardware profiles produce those sizes.
4. Agree to the car review policy. Make sure **App access** has sign-in details reviewers can use,
   since nothing plays until they sign in to a server (the demo server works).
5. Upload a first car bundle by hand (from a GitHub release) to an automotive testing track.
   Internal testing isn't reviewed, closed testing is reviewed without blocking, and open testing
   and production block on the review against Play's
   [car app quality guidelines](https://developer.android.com/docs/quality-guidelines/car-app-quality).
   See [Distribute to cars](https://developer.android.com/training/cars/distribute).
6. Set the repository variable: `gh variable set PLAY_AUTOMOTIVE_ENABLED --body true`.

Crashlytics: CI writes the same `google-services.json` into `app/automotive/`, since the package is
the same. Without one the car app builds without Firebase, as it does locally.

## Testing on an emulator

Install the **Android Automotive with Google APIs** (or Play Store) system image for API 35+ and
create an AVD for it. The car apps run as user 10, so pass `--user 10` to `am` and
`pm` commands.

```
./gradlew :app:automotive:assembleDebug
adb install -r app/automotive/build/outputs/apk/debug/automotive-debug.apk

# Open Campfire in the car's media app
adb shell am start -a android.car.intent.action.MEDIA_TEMPLATE \
  --es android.car.intent.extra.MEDIA_COMPONENT \
  app.campfire.android/app.campfire.audioplayer.impl.AudioPlayerService
```

From the emulator, a server on the host is at `http://10.0.2.2:<port>`. Use the testbed server
(`tools/testbed`), never a real one.

Debug builds can send voice requests the way an assistant does, without one:

```
adb shell "am broadcast --user 10 -n app.campfire.android/app.campfire.automotive.debug.VoiceSearchReceiver --es query 'project hail mary'"
```

Quote the whole command, as above, so the device shell doesn't split the query. An empty query is
a bare "play Campfire".
