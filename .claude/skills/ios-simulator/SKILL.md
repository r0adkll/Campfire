---
name: ios-simulator
description: Build Campfire for an iOS Simulator and drive it headlessly with AXe — tap, type, read the UI tree, screenshot, inspect app files and logs — against the throwaway testbed server. Use when verifying iOS behavior end to end, reproducing an iOS bug, or checking an iOS change in the running app.
---

# Driving Campfire on the iOS Simulator

[AXe](https://github.com/cameroncooke/AXe) drives a booted simulator through CoreSimulator, so no simulator window is needed: a UI dump takes about 0.2 s, and a tap or a typed string under 1 s. The server is always the testbed's (`tools/testbed/README.md`), never a real one.

## Which simulator

Work only on the simulator named **Campfire Testbed**, and pass its UDID explicitly to every `axe` and `simctl` command. Other simulators on the machine are the user's: one may be signed in to their real server. Physical devices (DeviceHub lists the user's iPhone, iPad and Watch) are off limits. That rules out `booted` as a target and any command that acts on all devices.

```bash
U=$(xcrun simctl list devices | sed -n 's/.*Campfire Testbed (\([0-9A-F-]*\)).*/\1/p' | head -1)
[ -n "$U" ] || U=$(xcrun simctl create "Campfire Testbed" "iPhone 18 Pro")
```

## Steps

1. **Tools.** Run `axe --version`. If AXe is missing, ask the user before running `brew install cameroncooke/axe/axe`. Done when `axe doctor --udid $U` reports `ready`. Run it after step 2 if the simulator wasn't booted yet.

2. **Server and simulator.**
   - Start the server with `tools/testbed/testbed.py up --no-device`. `status` prints its URL, credentials and pid. The simulator shares the Mac's network, so the app uses `http://localhost:<port>` (the default port is 13379).
   - Boot the simulator with `xcrun simctl boot $U`, unless it's already booted, and wait about 8 s before reading the UI: right after boot, the tree comes back as an empty application node.
   - Done when `curl localhost:<port>/ping` answers and `simctl list devices` shows the simulator Booted.

3. **Build and install** the checkout under test.

   ```bash
   DD=<scratchpad>/DerivedData
   xcodebuild -project app/ios/iosApp.xcodeproj -scheme iosApp -configuration Debug \
     -destination "id=$U" -derivedDataPath $DD build        # run in the background; minutes
   xcrun simctl install $U $DD/Build/Products/Debug-iphonesimulator/iosApp.app
   xcrun simctl launch $U com.r0adkll.campfire
   ```

   - The Run Script phase builds the Kotlin framework (`:app:ios:embedAndSignAppleFrameworkForXcode`).
   - iOS framework links sometimes hang. Compile with `./gradlew :app:ios:compileKotlinIosSimulatorArm64` first, and report a build that stalls instead of waiting on it.
   - `install` over an existing install keeps the app's data, which is how you test an upgrade.
   - Done when the build prints `** BUILD SUCCEEDED **` and `launch` prints a pid.

4. **Sign in.** Do this on the welcome screen, or on Add campsite in the account switcher.
   1. Tap "Add a campsite", tap the Server address field, and `type` the testbed URL.
   2. Wait for the form to expand, then re-dump.
   3. Debug builds pre-fill Username and Password with the developer's test credentials. Replace both: tap the field, press Cmd+A then backspace, and `type` the testbed's.
   4. Tap "Add campsite". On a first sign-in, also tap "Apply analytics consent".
   - Done when the dump shows "Continue Listening" or the bottom bar ("Home", "Library").

5. **Run the scenario, observing after every action.** Follow each action with a dump or a log check, and save a screenshot for each pass/fail observation. Done when every case has an observation that settles it.

6. **Tear down what you started.**
   - Run `testbed.py down` if your `up` started the server.
   - Kill any proxy you ran, and stop any `log stream` you started.
   - Shut down the simulator (`xcrun simctl shutdown $U`) if you booted it.
   - Done when nothing you started is still running.

## AXe

```bash
axe describe-ui --udid $U | python3 -I .claude/skills/ios-simulator/scripts/ui_tree.py   # on-screen nodes, centre points
axe tap --label 'Add campsite' --element-type Button --udid $U    # exact label match
axe tap --id button_download --udid $U                            # Compose testTag
axe tap -x 201 -y 467 --udid $U                                   # points, from the dump
axe type 'http://localhost:13379' --udid $U                       # into the focused field
axe key-combo --modifiers 227 --key 4 --udid $U && axe key 42 --udid $U   # select all, delete
axe key 40 --udid $U                                              # Return
axe swipe --start-x 350 --start-y 600 --end-x 50 --end-y 600 --udid $U
axe button home --udid $U                                         # background the app
xcrun simctl io $U screenshot <path>.png
```

Reading the tree:
- A Compose `testTag` is the node's `id`. Prefer `--id` when the dump shows one (for example `button_play`, `button_download`, `offline_status_card`, `item_actions_button`).
- Text fields are `TextArea`s, and a field's label gains its error text (`'Server address, Invalid address'`), so `--label` stops matching while an error shows. Tap the field by coordinates instead.
- Composite buttons have long labels (`'HomeLibraryItem, <title>, <title>, <author>'`). Tap their centre point.
- A dump taken mid-animation has stale frames. After anything that changes the screen, wait about 0.5 s and dump again before tapping by coordinates.
- `type` sends hardware key events. It works whether or not the software keyboard shows, and puts the text wherever the cursor is, so tap the field itself, not its label.
- `describe-ui` can restart `testmanagerd`, so use AXe alone, not alongside XCUITest or Maestro runs.

## Observing the app

- **Logs.**
  - Stream them with `xcrun simctl spawn $U log stream --level debug --style compact --predicate 'process == "iosApp"' > <file>`, in the background.
  - Campfire's own lines carry `(app_ios)` and a tag (`I/OfflineDownloads:`). Add `OR process == "nsurlsessiond"` to see background transfers.
- **App files** live under `$(xcrun simctl get_app_container $U com.r0adkll.campfire data)`.
  - Downloads are in `Library/Application Support/downloads/<itemId>/item/`. `xattr -l` on that directory shows the backup exclusion.
  - Compare a downloaded file's `md5` with the source file under `~/Libation/StoreSampleLibrary`.
  - A background transfer writes to `Library/Caches/com.apple.nsurlsessiond/Downloads/com.r0adkll.campfire/*.tmp` and reaches the app only once it finishes.
- **Server unreachable.** Use `kill -STOP <pid>`, then `kill -CONT <pid>`, with the pid from `testbed.py status`. Restarting the testbed re-seeds it with new ids and a new user, so sign in again after a restart.
- **Slow transfers.** Downloads over loopback finish instantly.
  - To catch one mid-flight, run `python3 -I .claude/skills/ios-simulator/scripts/throttle.py 13390 <port> 40000` in the background.
  - Then add a campsite at `http://127.0.0.1:13390`: about 40 KB/s, so roughly 90 s for a 3.5 MB book.
- **App lifecycle.**
  - `axe button home` suspends the app.
  - `xcrun simctl terminate $U com.r0adkll.campfire` stands in for the system killing it: background transfers continue, and iOS relaunches the app in the background to deliver them. Check that relaunch with `xcrun simctl spawn $U launchctl list | grep campfire`.
  - A transfer created while the app is in the background is discretionary, so iOS delays it.
