#!/usr/bin/env python3
"""A throwaway Audiobookshelf server + emulator for testing Campfire end to end. See tools/testbed/README.md.

State lives in tools/testbed/.work/state.json so separate invocations (an agent's successive shell
commands) share one running server and emulator until `down`.
"""
import argparse
import json
import os
import re
import sys
import time
import tomllib
from pathlib import Path

TOOL_DIR = Path(__file__).resolve().parent
WORK_DIR = TOOL_DIR / ".work"
STATE_PATH = WORK_DIR / "state.json"
sys.path.insert(0, str(TOOL_DIR.parent / "harness"))

from campfire_harness import emulator  # noqa: E402
from campfire_harness.app import App  # noqa: E402
from campfire_harness.config import load_app, load_device, load_fixture, load_server  # noqa: E402
from campfire_harness.proc import HarnessError, log, set_log_prefix  # noqa: E402
from campfire_harness.server import Fixture, Server  # noqa: E402

AVD_NAME = "campfire-testbed"


class Testbed:
    def __init__(self, spec_path: Path):
        with open(spec_path, "rb") as f:
            raw = tomllib.load(f)
        self.server_cfg = load_server(raw)
        self.fixture_spec = load_fixture(raw)
        self.app_cfg = load_app(raw)
        self.device = load_device(raw["device"], avd_name=AVD_NAME)
        self.server = Server(self.server_cfg, WORK_DIR)
        self.fixture = Fixture(self.fixture_spec, self.server_cfg, self.server.client)

    # -- state ---------------------------------------------------------------------------
    def load_state(self) -> dict:
        try:
            return json.loads(STATE_PATH.read_text())
        except (FileNotFoundError, json.JSONDecodeError):
            return {}

    def save_state(self, state: dict) -> None:
        WORK_DIR.mkdir(parents=True, exist_ok=True)
        STATE_PATH.write_text(json.dumps(state, indent=2))

    def server_running(self, state: dict) -> bool:
        pid = state.get("server_pid")
        if not pid:
            return False
        try:
            os.kill(pid, 0)
        except ProcessLookupError:
            return False
        return self.server.is_up()

    def require_server(self) -> None:
        if not self.server_running(self.load_state()):
            raise HarnessError("No testbed server is running; start one with `testbed.py up`")
        self.fixture.attach()

    def adb(self) -> emulator.Adb:
        serial = emulator.find_running(self.device)
        if not serial:
            raise HarnessError(f"The {AVD_NAME} emulator isn't running; start it with `testbed.py up`")
        return emulator.Adb(serial)

    def app(self) -> App:
        return App(self.app_cfg, self.server_cfg, self.adb())

    def status(self) -> dict:
        state = self.load_state()
        running = self.server_running(state)
        info = {
            "server": {
                "running": running,
                "url_for_host": self.server_cfg.url_for_host,
                "url_for_emulator": self.server_cfg.url_for_emulator,
                "username": self.server_cfg.username,
                "password": self.server_cfg.password,
                "pid": state.get("server_pid") if running else None,
                "log": str(self.server.log_path),
            },
            "emulator": {"avd": AVD_NAME, "serial": emulator.find_running(self.device)},
            "app": {"package": self.app_cfg.package, "variant": self.app_cfg.variant},
        }
        if running:
            self.fixture.attach()
            info["server"]["libraries"] = self.fixture.library_ids
        return info

    # -- lifecycle -----------------------------------------------------------------------
    def up(self, args) -> None:
        state = self.load_state()
        if self.server_running(state) and not args.fresh:
            log(f"Reusing running server (pid {state['server_pid']})")
            self.fixture.attach()
        else:
            if state.get("server_pid"):
                self.server.stop_pid(state["server_pid"])
            self.server.ensure_checkout()
            self.server.start()
            state["server_pid"] = self.server.proc.pid
            self.save_state(state)
            self.fixture.apply()
        if args.no_device:
            return

        adb = emulator.boot(self.device, log_path=WORK_DIR / "emulator.log", cold=args.cold, headless=not args.window)
        emulator.prepare(adb)
        app = App(self.app_cfg, self.server_cfg, adb)
        app.build_and_install(skip_build=args.skip_build, clear_data=not args.keep_data)
        self.sign_in(app, self.app_cfg.library)

    def sign_in(self, app: App, library: str) -> None:
        def send_setup():
            app.setup(library=library, theme_mode=self.app_cfg.theme_mode, theme=self.app_cfg.theme)

        send_setup()
        time.sleep(6)  # login + initial sync
        app.wait_for_home(resend_setup=send_setup)
        log("App is signed in to the testbed server")

    def down(self, args) -> None:
        state = self.load_state()
        if state.get("server_pid"):
            log("Stopping Audiobookshelf")
            self.server.stop_pid(state["server_pid"])
        serial = emulator.find_running(self.device)
        if serial and not args.keep_emulator:
            emulator.stop(emulator.Adb(serial))
        STATE_PATH.unlink(missing_ok=True)


def print_json(value) -> None:
    print(json.dumps(value, indent=2))


def parse_args(argv):
    p = argparse.ArgumentParser(description="Throwaway Audiobookshelf server + emulator for testing Campfire.")
    p.add_argument("--spec", type=Path, default=TOOL_DIR / "testbed.toml")
    sub = p.add_subparsers(dest="command", required=True)

    up = sub.add_parser("up", help="Start (or reuse) the server + Fixture, boot the emulator, install and sign in")
    up.add_argument("--no-device", action="store_true", help="Server only")
    up.add_argument("--fresh", action="store_true", help="Restart the server from an empty data dir")
    up.add_argument("--skip-build", action="store_true", help="Install the already-built APK")
    up.add_argument("--keep-data", action="store_true", help="Don't clear app data on install")
    up.add_argument("--window", action="store_true", help="Show the emulator window (default: headless)")
    up.add_argument("--cold", action="store_true", help="Cold-boot the emulator")

    down = sub.add_parser("down", help="Stop the server and the emulator")
    down.add_argument("--keep-emulator", action="store_true")

    sub.add_parser("status", help="Print URLs, credentials, library ids, emulator serial (JSON)")

    api = sub.add_parser("api", help="Authenticated request to the server, e.g. `api GET /api/libraries`")
    api.add_argument("method")
    api.add_argument("path")
    api.add_argument("body", nargs="?", help="JSON body")

    item = sub.add_parser("item", help="Resolve a title to its library item (JSON)")
    item.add_argument("title")

    scan = sub.add_parser("scan", help="Scan Fixture libraries and wait for the scan to finish")
    scan.add_argument("libraries", nargs="*", help="Library names (default: all)")

    app = sub.add_parser("app", help="Drive the installed app")
    app_sub = app.add_subparsers(dest="action", required=True)
    install = app_sub.add_parser("install", help="Build (unless --skip-build) and install the app")
    install.add_argument("--skip-build", action="store_true")
    install.add_argument("--keep-data", action="store_true", help="Upgrade in place instead of clearing data")
    signin = app_sub.add_parser("sign-in", help="Send the setup intent and wait for a signed-in Home")
    signin.add_argument("--library")
    app_sub.add_parser("launch")
    app_sub.add_parser("restart", help="Force-stop and cold-start the app")
    app_sub.add_parser("stop", help="Force-stop the app")
    nav = app_sub.add_parser("navigate", help="home, library, series, authors, collections, playlists, "
                                              "statistics, settings, library_item (arg = item id)")
    nav.add_argument("screen")
    nav.add_argument("arg", nargs="?")
    open_item = app_sub.add_parser("open", help="Open a library item's detail screen by title")
    open_item.add_argument("title")
    play = app_sub.add_parser("play", help="Start playback of a title and wait until it's playing")
    play.add_argument("title")
    app_sub.add_parser("stop-playback")
    ui = app_sub.add_parser("ui", help="List on-screen text / content-descriptions")
    ui.add_argument("--grep", help="Only labels matching this regex")
    tap = app_sub.add_parser("tap", help="Tap the first node whose text / content-description matches")
    tap.add_argument("pattern")
    wait_for = app_sub.add_parser("wait-for", help="Block until a node matches")
    wait_for.add_argument("pattern")
    wait_for.add_argument("--timeout", type=int, default=20_000, help="ms")
    swipe = app_sub.add_parser("swipe")
    swipe.add_argument("direction", choices=["up", "down"])
    swipe.add_argument("--times", type=int, default=1)
    app_sub.add_parser("back")
    shot = app_sub.add_parser("screencap", help="Save a PNG of the screen")
    shot.add_argument("path", type=Path)
    logcat = app_sub.add_parser("logcat", help="Recent log lines from the app process")
    logcat.add_argument("--lines", type=int, default=500)
    logcat.add_argument("--grep", help="Only lines matching this regex")
    return p.parse_args(argv)


def run_app(tb: Testbed, args) -> None:
    action = args.action
    if action in ("sign-in", "open", "play"):
        tb.require_server()
    app = tb.app()
    if action == "install":
        app.build_and_install(skip_build=args.skip_build, clear_data=not args.keep_data)
    elif action == "sign-in":
        tb.sign_in(app, args.library or tb.app_cfg.library)
    elif action == "launch":
        app.launch()
    elif action == "restart":
        app.restart()
    elif action == "stop":
        app.stop()
    elif action == "navigate":
        app.navigate(args.screen, args.arg)
    elif action == "open":
        app.navigate("library_item", tb.fixture.find_book(args.title)["id"])
    elif action == "play":
        app.play(tb.fixture.find_book(args.title)["id"])
    elif action == "stop-playback":
        app.stop_playback()
    elif action == "ui":
        rx = re.compile(args.grep, re.IGNORECASE) if args.grep else None
        for label, bounds in app.ui_labels():
            if rx is None or rx.search(label):
                print(f"{label}\t{bounds}")
    elif action == "tap":
        app.tap(args.pattern)
    elif action == "wait-for":
        app.wait_for(args.pattern, args.timeout)
    elif action == "swipe":
        app.swipe(args.direction, args.times)
    elif action == "back":
        app.back()
    elif action == "screencap":
        app.screencap(args.path)
        print(args.path.resolve())
    elif action == "logcat":
        rx = re.compile(args.grep, re.IGNORECASE) if args.grep else None
        for line in app.logcat(lines=args.lines).splitlines():
            if rx is None or rx.search(line):
                print(line)


def main(argv=None) -> int:
    set_log_prefix("testbed")
    args = parse_args(argv)
    tb = Testbed(args.spec)
    if args.command == "up":
        tb.up(args)
        print_json(tb.status())
    elif args.command == "down":
        tb.down(args)
    elif args.command == "status":
        print_json(tb.status())
    elif args.command == "api":
        tb.require_server()
        body = json.loads(args.body) if args.body else None
        print_json(tb.server.client.request(args.method.upper(), args.path, body))
    elif args.command == "item":
        tb.require_server()
        item = tb.fixture.find_book(args.title)
        print_json({"id": item["id"], "libraryId": item["libraryId"],
                    "title": item.get("media", {}).get("metadata", {}).get("title")})
    elif args.command == "scan":
        tb.require_server()
        names = args.libraries or list(tb.fixture.library_ids)
        unknown = set(names) - set(tb.fixture.library_ids)
        if unknown:
            raise HarnessError(f"Unknown libraries {sorted(unknown)}; known: {list(tb.fixture.library_ids)}")
        tb.fixture.scan([tb.fixture.library_ids[n] for n in names])
    elif args.command == "app":
        run_app(tb, args)
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except HarnessError as e:
        log(f"ERROR: {e}")
        sys.exit(1)
    except KeyboardInterrupt:
        sys.exit(130)
