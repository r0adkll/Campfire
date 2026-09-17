#!/usr/bin/env python3
"""Capture store screenshots for one Device Class. See tools/screenshots/README.md."""
import argparse
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "harness"))

from campfire_shots import chrome  # noqa: E402
from campfire_shots.config import TOOL_DIR, WORK_DIR, Shot, load_spec, pinned_now  # noqa: E402
from campfire_shots.output import write_shots  # noqa: E402
from campfire_harness import emulator  # noqa: E402
from campfire_harness.app import App  # noqa: E402
from campfire_harness.proc import HarnessError, log, run, set_log_prefix  # noqa: E402
from campfire_harness.server import Fixture, Server  # noqa: E402


def parse_args(argv):
    p = argparse.ArgumentParser(description="Reproducible store screenshots for Campfire (Android).")
    p.add_argument("--spec", type=Path, default=TOOL_DIR / "shots.toml")
    p.add_argument("--class", dest="device_class", required=True, help="Device class key from the spec (phone, seven, ten)")
    p.add_argument("--shots", help="Comma-separated shot names; default = every enabled shot for the class")
    p.add_argument("--locale", help="Locale to capture in (default: [app].locale)")
    p.add_argument("--out", type=Path, help="Write PNGs here instead of Store Metadata")
    p.add_argument("--cold", action="store_true", help="Cold-boot the emulator instead of loading a snapshot")
    p.add_argument("--headless", action="store_true", help="Boot the emulator without a window")
    p.add_argument("--skip-build", action="store_true", help="Install the already-built APK")
    p.add_argument("--keep-server", action="store_true", help="Leave the Audiobookshelf server running")
    p.add_argument("--keep-emulator", action="store_true", help="Leave the emulator running after the run")
    p.add_argument("--regenerate", action="store_true", help="Re-run the Sample Library generator first")
    p.add_argument("--server-only", action="store_true",
                   help="Start the server, apply the Fixture, then wait (Ctrl-C to stop). For troubleshooting.")
    return p.parse_args(argv)


def run_steps(app: App, fixture: Fixture, shot: Shot, settle_ms: int) -> None:
    for step in shot.steps:
        if "navigate" in step:
            arg = step.get("arg")
            if "title" in step:
                arg = fixture.find_book(step["title"])["id"]
            app.navigate(step["navigate"], arg)
        elif "play" in step:
            app.play(fixture.find_book(step["play"])["id"])
        elif step.get("expand_player"):
            app.expand_player()
        elif step.get("stop_playback"):
            app.stop_playback()
        elif "wait_for" in step:
            app.wait_for(step["wait_for"], int(step.get("timeout", 20000)))
        elif "tap" in step:
            app.tap(step["tap"])
        elif "type" in step:
            app.type_text(step["type"])
        elif "swipe" in step:
            app.swipe(step["swipe"], int(step.get("times", 1)), float(step.get("distance", 0.5)))
        elif "wait" in step:
            time.sleep(int(step["wait"]) / 1000)
            continue
        elif step.get("hide_keyboard"):
            app.hide_keyboard()
        elif step.get("back"):
            app.back()
        else:
            raise HarnessError(f"Shot '{shot.name}': unknown step {step}")
        time.sleep(0.8)
    time.sleep(settle_ms / 1000)


def main(argv=None) -> int:
    set_log_prefix("shots")
    args = parse_args(argv)
    spec = load_spec(args.spec)
    if args.device_class not in spec.classes:
        raise HarnessError(f"Unknown class '{args.device_class}'; known: {list(spec.classes)}")
    cls = spec.classes[args.device_class]
    names = [n.strip() for n in args.shots.split(",")] if args.shots else None
    shots = spec.shots_for(cls.key, names)
    if not shots:
        raise HarnessError(f"No shots to capture for class '{cls.key}'")
    locale = args.locale or spec.app.raw.get("locale", "en-US")
    log(f"Class {cls.key}: {[s.name for s in shots]} @ {locale}")

    WORK_DIR.mkdir(parents=True, exist_ok=True)
    if args.regenerate:
        if not spec.fixture.regenerate_cmd:
            raise HarnessError("--regenerate given but [sample_library].regenerate is not set")
        log("Regenerating the Sample Library…")
        run(["sh", "-c", spec.fixture.regenerate_cmd], cwd=str(spec.fixture.sample_library_path))

    server = Server(spec.server, WORK_DIR)
    server.ensure_checkout()
    server.start()
    adb = None
    try:
        fixture = Fixture(spec.fixture, spec.server, server.client, now=pinned_now())
        fixture.apply()
        if args.server_only:
            log(f"Server ready at {spec.server.url_for_host} (emulator: {spec.server.url_for_emulator}); Ctrl-C to stop")
            while True:
                time.sleep(3600)

        emulator.stop_other_emulators(cls.device, prefix="campfire-shots-")
        adb = emulator.boot(cls.device, log_path=WORK_DIR / f"emulator-{cls.key}.log",
                            cold=args.cold, headless=args.headless)
        chrome.set_locale(adb, locale)
        chrome.prepare(adb)

        app = App(spec.app, spec.server, adb)
        app.build_and_install(skip_build=args.skip_build)

        default_library = spec.app.library
        def send_setup():
            app.setup(library=default_library, theme_mode=spec.app.theme_mode, theme=spec.app.theme)

        send_setup()
        time.sleep(6)  # login + initial sync
        app.wait_for_home(resend_setup=send_setup)

        captures_dir = WORK_DIR / "captures" / cls.key / locale
        captured = []
        for shot in shots:
            log(f"Shot: {shot.name}")
            library = shot.library or default_library
            if shot.library or shot.theme_mode or shot.theme:
                app.setup(library=library, theme_mode=shot.theme_mode or spec.app.theme_mode,
                          theme=shot.theme or spec.app.theme)
                time.sleep(2)
            run_steps(app, fixture, shot, shot.settle_ms or int(spec.app.raw.get("settle_ms", 2000)))
            chrome.set_clock(adb)
            time.sleep(0.5)
            png = captures_dir / f"{shot.name}.png"
            app.screencap(png)
            captured.append((shot.name, png))
            if shot.library or shot.theme_mode or shot.theme:
                app.setup(library=default_library, theme_mode=spec.app.theme_mode, theme=spec.app.theme)
                time.sleep(2)
            # Reset to a neutral state so shots don't leak into each other
            app.stop_playback()
            app.navigate("home")
            time.sleep(1)

        if args.out:
            args.out.mkdir(parents=True, exist_ok=True)
            for name, png in captured:
                (args.out / f"{name}.png").write_bytes(png.read_bytes())
            log(f"Wrote {len(captured)} PNGs to {args.out}")
        else:
            write_shots(spec, cls, locale, captured, replace_all=names is None)
    finally:
        if adb is not None and not args.keep_emulator:
            emulator.stop(adb)
        if args.keep_server:
            log(f"Leaving server running at {spec.server.url_for_host} (pid {server.proc.pid if server.proc else '?'})")
        else:
            server.stop()
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except HarnessError as e:
        log(f"ERROR: {e}")
        sys.exit(1)
    except KeyboardInterrupt:
        sys.exit(130)
