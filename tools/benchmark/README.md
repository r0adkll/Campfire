# Benchmark

Measures build time and runtime startup cost, archived per label so two branches can be compared
— built to measure the cost of dependency injection (e.g. before and after a DI framework change).

## Build time

Uses [Gradle Profiler](https://github.com/gradle/gradle-profiler) (`brew install gradle-profiler`)
with the scenarios in `build.scenarios`: clean and incremental (ABI / non-ABI) builds of the
Android app and the desktop app.

```bash
tools/benchmark/run.py build --label before                  # every default scenario
tools/benchmark/run.py build --label before android_clean    # just the named scenarios
tools/benchmark/run.py build --label before ios_clean        # Kotlin/Native, not a default
```

Close Android Studio and other Gradle builds first; the whole default set takes around an hour.
Gradle Profiler reverts the source edits it makes, but don't edit the tree while it runs.

## Runtime

Runs `DiStartupBenchmarks` (`:app:baselineprofile`) on the connected device: cold starts of the
`fossBenchmarkRelease` build (`--flavor` to change; the Firebase flavors need a
`google-services.json` to start), fully AOT-compiled, signed in and launching to Home
content, recording startup timing, memory, and the time spent building each dependency graph
(the `di:*` trace sections from `DiTraceSections`). It signs in once with the login form's
prefilled test credentials (`campfire_server_url`, `campfire_username`, `campfire_password` in
`~/.gradle/gradle.properties`). Needs exactly one device attached; prefer a physical device, since
emulator timings are noisy. The benchmark build installs as `app.campfire.android.benchmark`,
alongside any real install.

```bash
tools/benchmark/run.py runtime --label before
tools/benchmark/run.py runtime --label before --emulator --iterations 30
```

The Perfetto traces are archived next to the results for digging into a regression.

## Comparing

```bash
tools/benchmark/run.py compare before after
```

Prints Markdown tables of the medians for every scenario and metric both labels recorded, ready to
paste into a PR. Results live in `tools/benchmark/.results/<build|runtime>/<label>/` (gitignored).
Only compare runs from the same machine or device.
