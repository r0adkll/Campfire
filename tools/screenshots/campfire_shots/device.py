"""Drive the installed app on an emulator: install, deep-link intents, UI steps, capture."""
import glob
import re
import time
import xml.etree.ElementTree as ET
from pathlib import Path

from .config import REPO_ROOT, Spec
from .emulator import Adb
from .proc import wait_until, ShotError, log, run

MAIN = "app.campfire.android.MainActivity"


class App:
    def __init__(self, spec: Spec, adb: Adb, server_url: str):
        self.spec = spec
        self.adb = adb
        self.server_url = server_url
        self.package = spec.app["package"]
        self.activity = spec.app.get("activity", MAIN)
        self.variant = spec.app["variant"]

    # -- install -------------------------------------------------------------------------
    def build_and_install(self, *, skip_build: bool = False) -> None:
        flavor = self.variant.replace("Debug", "")
        if not skip_build:
            log(f"Building {self.variant} APK")
            # Never bake the developer's login prefill (~/.gradle/gradle.properties) into this APK:
            # the signed-out screen seen during setup must not leak the server address or username.
            run([str(REPO_ROOT / "gradlew"), f":app:android:assemble{self.variant[0].upper()}{self.variant[1:]}",
                 "-Pcampfire_no_test_credentials=true", "-q"], cwd=str(REPO_ROOT))
        pattern = str(REPO_ROOT / "app" / "android" / "build" / "outputs" / "apk" / flavor / "debug" / "*.apk")
        apks = glob.glob(pattern)
        if not apks:
            raise ShotError(f"No APK found at {pattern}")
        log(f"Installing {Path(apks[0]).name}")
        self.adb("install", "-r", "-g", apks[0], timeout=300)
        self.adb.shell("pm", "clear", self.package)
        for perm in ("android.permission.POST_NOTIFICATIONS", "android.permission.ACCESS_LOCAL_NETWORK"):
            self.adb.shell("pm", "grant", self.package, perm, check=False)

    # -- intents -------------------------------------------------------------------------
    def _start(self, *extras: str) -> None:
        self.adb.shell("am", "start", "-W", "-n", f"{self.package}/{self.activity}",
                       "-a", "android.intent.action.MAIN", *extras)

    def launch(self) -> None:
        self._start()

    def setup(self, *, library: str, theme_mode: str | None, theme: str | None) -> None:
        cfg = self.spec.server
        extras = [
            "--es", "campfire_action", "setup",
            "--es", "campfire_server_url", self.server_url,
            "--es", "campfire_server_name", cfg.server_name,
            "--es", "campfire_username", cfg.username,
            "--es", "campfire_password", cfg.password,
            "--es", "campfire_library_name", library,
        ]
        if theme_mode:
            extras += ["--es", "campfire_theme_mode", theme_mode]
        if theme:
            extras += ["--es", "campfire_theme", theme]
        self._start(*extras)

    def wait_for_home(self, *, timeout: float = 60, resend_setup=None) -> None:
        """Wait until the app shows a signed-in Home. Dismisses an ANR dialog and re-sends the
        setup intent (via `resend_setup`) if the app is still on the Welcome screen."""
        deadline = time.monotonic() + timeout
        last_resend = 0.0
        while time.monotonic() < deadline:
            labels = []
            try:
                for node in self._ui_nodes():
                    labels.append(node.get("text", "") + "\n" + node.get("content-desc", ""))
            except Exception:
                labels = []
            joined = "\n".join(labels)
            if "isn't responding" in joined:
                log("App not responding; choosing Wait")
                self.tap("^Wait$")
                time.sleep(3)
                continue
            if "Add a campsite" in joined and resend_setup and time.monotonic() - last_resend > 10:
                log("Still on Welcome; re-sending setup")
                resend_setup()
                last_resend = time.monotonic()
                time.sleep(5)
                continue
            if "Continue Listening" in joined or "Recently Added" in joined or "\nSearch" in joined:
                return
            time.sleep(2)
        raise ShotError("App did not reach a signed-in Home in time (see adb logcat)")

    def navigate(self, screen: str, arg: str | None = None) -> None:
        extras = ["--es", "campfire_action", "navigate", "--es", "campfire_screen", screen]
        if arg is not None:
            extras += ["--es", "campfire_screen_arg", arg]
        self._start(*extras)

    def play(self, item_id: str, timeout_ms: int = 30_000) -> None:
        """Start playback and block until the platform media session reports PLAYING. The mini player
        is not in the uiautomator tree, so the session state is the only reliable signal."""
        self._start("--es", "campfire_action", "play", "--es", "library_item_id", item_id)
        wait_until(self.is_playing, timeout=timeout_ms / 1000, interval=0.5, what="playback to start")

    def is_playing(self) -> bool:
        dump = self.adb.shell("dumpsys", "media_session", check=False)
        in_app = False
        for line in dump.splitlines():
            if "package=" in line:
                in_app = f"package={self.package}" in line
            if in_app and "state=PLAYING" in line:
                return True
        return False

    def expand_player(self) -> None:
        self._start("--es", "campfire_action", "expand_player")

    def stop_playback(self) -> None:
        self._start("--es", "campfire_action", "stop_playback")

    def stop(self) -> None:
        self.adb.shell("am", "force-stop", self.package)

    # -- UI ------------------------------------------------------------------------------
    def _ui_nodes(self):
        self.adb.shell("uiautomator", "dump", "/sdcard/campfire-ui.xml")
        xml = self.adb.raw("exec-out", "cat", "/sdcard/campfire-ui.xml").decode(errors="replace")
        return ET.fromstring(xml).iter("node")

    def _find_node(self, pattern: str):
        rx = re.compile(pattern, re.IGNORECASE)
        for node in self._ui_nodes():
            labels = (node.get("text", ""), node.get("content-desc", ""))
            if any(label and rx.search(label) for label in labels):
                return node
        return None

    def tap(self, pattern: str) -> None:
        for _ in range(3):
            node = self._find_node(pattern)
            if node is not None:
                x1, y1, x2, y2 = map(int, re.findall(r"-?\d+", node.get("bounds", "")))
                self.adb.shell("input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))
                return
            time.sleep(1)
        raise ShotError(f"tap: no on-screen node matching /{pattern}/")

    def wait_for(self, pattern: str, timeout_ms: int = 20_000) -> None:
        """Block until a node whose text / content-description matches `pattern` is on screen."""
        wait_until(lambda: self._find_node(pattern) is not None, timeout=timeout_ms / 1000,
                   interval=0.5, what=f"an on-screen node matching /{pattern}/")

    def type_text(self, text: str) -> None:
        self.adb.shell("input", "text", text.replace(" ", "%s"))

    def swipe(self, direction: str, times: int = 1, distance: float = 0.5) -> None:
        """Scroll by `distance` (fraction of screen height) per swipe."""
        size = self.adb.shell("wm", "size").strip().split()[-1]
        w, h = map(int, size.split("x"))
        x = w // 2
        mid = h // 2
        half = int(h * distance / 2)
        y_from, y_to = (mid + half, mid - half) if direction == "up" else (mid - half, mid + half)
        for _ in range(times):
            self.adb.shell("input", "swipe", str(x), str(y_from), str(x), str(y_to), "400")
            time.sleep(0.6)

    def hide_keyboard(self) -> None:
        self.adb.shell("input", "keyevent", "KEYCODE_ESCAPE")

    def back(self) -> None:
        self.adb.shell("input", "keyevent", "KEYCODE_BACK")

    def screencap(self, dest: Path) -> None:
        png = self.adb.raw("exec-out", "screencap", "-p")
        if not png.startswith(b"\x89PNG"):
            raise ShotError("screencap did not return a PNG")
        dest.parent.mkdir(parents=True, exist_ok=True)
        dest.write_bytes(png)
