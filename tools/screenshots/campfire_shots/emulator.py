"""Create, boot, and prepare the per-Device-Class emulators."""
import os
import shlex
import subprocess
import time
from pathlib import Path

from .config import DeviceClass, WORK_DIR
from .proc import ShotError, log, out, run, wait_until, which

SDK = Path(os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT") or "~/Library/Android/sdk").expanduser()
AVD_HOME = Path(os.environ.get("ANDROID_AVD_HOME") or "~/.android/avd").expanduser()


def adb_path() -> str:
    return which("adb", str(SDK / "platform-tools" / "adb"))


def emulator_path() -> str:
    return which("emulator", str(SDK / "emulator" / "emulator"))


class Adb:
    def __init__(self, serial: str):
        self.adb = adb_path()
        self.serial = serial

    def __call__(self, *args, check=True, timeout=120) -> str:
        return out([self.adb, "-s", self.serial, *args], check=check, timeout=timeout)

    def shell(self, *args, check=True, timeout=120) -> str:
        # adb joins its arguments with spaces and hands the string to the device shell, so every
        # argument must be quoted here or values with spaces split apart on the device.
        return self("shell", " ".join(shlex.quote(str(a)) for a in args), check=check, timeout=timeout)

    def raw(self, *args, timeout=120) -> bytes:
        return run([self.adb, "-s", self.serial, *args], capture=True, timeout=timeout).stdout


def ensure_avd(cls: DeviceClass) -> None:
    """Write the AVD files directly (no avdmanager needed) if the AVD doesn't exist."""
    avd_dir = AVD_HOME / f"{cls.avd_name}.avd"
    ini = AVD_HOME / f"{cls.avd_name}.ini"
    if avd_dir.exists() and ini.exists():
        return

    parts = cls.system_image.split(";")  # system-images;android-36;google_apis;arm64-v8a
    if len(parts) != 4:
        raise ShotError(f"Bad system_image '{cls.system_image}' (expected system-images;android-N;tag;abi)")
    _, platform, tag, abi = parts
    sysdir = SDK / "system-images" / platform / tag / abi
    if not (sysdir / "system.img").exists():
        raise ShotError(
            f"System image not installed: {sysdir}\n"
            f"Install with: sdkmanager \"{cls.system_image}\"  (or Android Studio > SDK Manager)"
        )

    log(f"Creating AVD {cls.avd_name} ({cls.width}x{cls.height}@{cls.density}, {cls.system_image})")
    AVD_HOME.mkdir(parents=True, exist_ok=True)
    avd_dir.mkdir(parents=True, exist_ok=True)
    arch = "arm64" if abi.startswith("arm64") else "x86_64"
    config = {
        "AvdId": cls.avd_name,
        "avd.ini.displayname": cls.avd_name,
        "avd.ini.encoding": "UTF-8",
        "abi.type": abi,
        "hw.cpu.arch": arch,
        "hw.cpu.ncore": "4",
        "hw.ramSize": "3072",
        "vm.heapSize": "512",
        "disk.dataPartition.size": "6G",
        "sdcard.size": "512M",
        "image.sysdir.1": f"system-images/{platform}/{tag}/{abi}/",
        "tag.id": tag,
        "tag.display": tag,
        "target": platform,
        "hw.lcd.width": str(cls.width),
        "hw.lcd.height": str(cls.height),
        "hw.lcd.density": str(cls.density),
        "hw.initialOrientation": cls.orientation,
        "skin.name": f"{cls.width}x{cls.height}",
        "skin.path": "_no_skin",
        "skin.dynamic": "yes",
        "showDeviceFrame": "no",
        "hw.keyboard": "yes",
        "hw.mainKeys": "no",
        "hw.gpu.enabled": "yes",
        "hw.gpu.mode": "auto",
        "hw.audioInput": "no",
        "hw.camera.back": "none",
        "hw.camera.front": "none",
        "hw.gps": "no",
        "hw.sensors.orientation": "yes",
        "hw.accelerometer": "yes",
        "fastboot.forceFastBoot": "yes",
        "runtime.network.latency": "none",
        "runtime.network.speed": "full",
        "PlayStore.enabled": "false",
    }
    (avd_dir / "config.ini").write_text("".join(f"{k}={v}\n" for k, v in config.items()))
    ini.write_text(
        "avd.ini.encoding=UTF-8\n"
        f"path={avd_dir}\n"
        f"path.rel=avd/{cls.avd_name}.avd\n"
        f"target={platform}\n"
    )


def _running_serials() -> list[str]:
    lines = out([adb_path(), "devices"]).splitlines()[1:]
    return [l.split("\t")[0] for l in lines if l.strip().endswith("device")]


def _avd_name_of(serial: str) -> str | None:
    try:
        text = out([adb_path(), "-s", serial, "emu", "avd", "name"], timeout=10)
    except ShotError:
        return None
    for line in text.splitlines():
        line = line.strip()
        if line and line != "OK":
            return line
    return None


def stop_other_shot_emulators(cls: DeviceClass) -> None:
    """Stop other `campfire-shots-*` emulators (never personal AVDs) to keep the host responsive."""
    for serial in _running_serials():
        if not serial.startswith("emulator-"):
            continue
        name = _avd_name_of(serial)
        if name and name.startswith("campfire-shots-") and name != cls.avd_name:
            log(f"Stopping {name} ({serial})")
            run([adb_path(), "-s", serial, "emu", "kill"], check=False, capture=True)
    time.sleep(2)


def stop(adb: Adb) -> None:
    log(f"Stopping emulator {adb.serial}")
    run([adb.adb, "-s", adb.serial, "emu", "kill"], check=False, capture=True)
    try:
        wait_until(lambda: adb.serial not in _running_serials(), timeout=45, interval=2,
                   what="emulator to shut down")
    except ShotError as e:
        log(f"WARNING: {e}")


def find_running(cls: DeviceClass) -> str | None:
    for serial in _running_serials():
        if serial.startswith("emulator-") and _avd_name_of(serial) == cls.avd_name:
            return serial
    return None


def boot(cls: DeviceClass, *, cold: bool = False, headless: bool = False) -> Adb:
    serial = find_running(cls)
    if serial:
        log(f"Reusing running emulator {serial} ({cls.avd_name})")
        return Adb(serial)

    ensure_avd(cls)
    before = set(_running_serials())
    cmd = [emulator_path(), "-avd", cls.avd_name, "-no-boot-anim", "-netdelay", "none", "-netspeed", "full"]
    if cold:
        cmd.append("-no-snapshot-load")
    if headless:
        cmd.append("-no-window")
    WORK_DIR.mkdir(parents=True, exist_ok=True)
    logf = open(WORK_DIR / f"emulator-{cls.key}.log", "wb")
    log(f"Booting {cls.avd_name}…")
    subprocess.Popen(cmd, stdout=logf, stderr=subprocess.STDOUT, start_new_session=True)

    def appeared():
        new = set(_running_serials()) - before
        for s in new:
            if _avd_name_of(s) == cls.avd_name:
                return s
        return None

    serial = None

    def _check():
        nonlocal serial
        serial = appeared()
        return serial is not None

    wait_until(_check, timeout=180, interval=2, what="emulator to appear in adb")
    adb = Adb(serial)
    wait_until(lambda: adb.shell("getprop", "sys.boot_completed", check=False).strip() == "1",
               timeout=300, interval=3, what="emulator to finish booting")
    time.sleep(3)
    return adb


def prepare(adb: Adb) -> None:
    """Deterministic chrome: demo-mode status bar, no animations, screen on, portrait/landscape locked."""
    # Window/transition animations off so captures don't catch a transition. The animator scale
    # stays at 1: at 0, Compose InfiniteTransitions (e.g. the search empty state) spin the main
    # thread and ANR the app.
    for key in ("window_animation_scale", "transition_animation_scale"):
        adb.shell("settings", "put", "global", key, "0")
    adb.shell("settings", "put", "global", "animator_duration_scale", "1")
    adb.shell("settings", "put", "system", "screen_off_timeout", "1800000")
    adb.shell("settings", "put", "system", "accelerometer_rotation", "0")
    adb.shell("settings", "put", "secure", "immersive_mode_confirmations", "confirmed")
    # No soft keyboard in captures: the AVD has a hardware keyboard (hw.keyboard=yes) and the soft
    # keyboard is told not to show alongside it. `input text` injects key events directly.
    adb.shell("settings", "put", "secure", "show_ime_with_hard_keyboard", "0")
    adb.shell("input", "keyevent", "KEYCODE_WAKEUP")
    adb.shell("wm", "dismiss-keyguard")

    # SystemUI demo mode for the clock / battery / notification shade. The `network` demo command
    # is deliberately NOT used: on the android-36 google_apis image every call renders a broken
    # glyph that accumulates. Instead the cellular radio is silenced via the emulator console and
    # Wi-Fi is left connected, which yields a bar with just the clock and a full battery.
    # Status bar. SystemUI demo mode is NOT used: on the android-36 google_apis image every demo
    # status event renders a broken glyph that accumulates. Instead the real bar is shaped:
    # clock pinned via root `date` (auto time off), full battery on the emulator console, full
    # cellular signal, then SystemUI restarted so no stale icons survive a previous run.
    adb("root", check=False)
    time.sleep(1)
    # No soft keyboard: disable the real keyboards and select the voice IME (which draws nothing).
    # Disabling alone is not enough — the system re-enables a default IME on first text input.
    imes = adb.shell("ime", "list", "-s", check=False).split()
    voice = [i for i in imes if "voice" in i.lower() or "tts" in i.lower()]
    for ime in imes:
        if ime not in voice:
            adb.shell("ime", "disable", ime, check=False)
    if voice:
        adb.shell("ime", "set", voice[0], check=False)
    adb.shell("settings", "put", "global", "auto_time", "0")
    adb.shell("settings", "put", "global", "auto_time_zone", "0")
    adb.shell("settings", "put", "global", "sysui_demo_allowed", "0")
    adb.shell("am", "broadcast", "-a", "com.android.systemui.demo", "-e", "command", "exit", check=False)
    adb("emu", "power", "ac", "off", check=False)
    adb("emu", "power", "capacity", "100", check=False)
    adb("emu", "gsm", "voice", "home", check=False)
    adb("emu", "gsm", "data", "home", check=False)
    adb("emu", "gsm", "signal-profile", "4", check=False)
    set_clock(adb)
    # Hide the "USB debugging connected" notification icon
    adb.shell("setprop", "persist.adb.notify", "0", check=False)
    adb.shell("pkill", "-f", "com.android.systemui", check=False)
    time.sleep(6)
    # Hide notification icons (ADB debugging etc.) from the freshly restarted bar
    adb.shell("cmd", "statusbar", "send-disable-flag", "notification-icons", check=False)


def set_clock(adb: Adb, hhmm: str = "1200") -> None:
    """Pin the device clock (needs root). Called before every capture so it never drifts."""
    adb.shell("date", f"0821{hhmm}2026.00", check=False)
    adb.shell("am", "broadcast", "-a", "android.intent.action.TIME_SET", check=False)


def current_locale(adb: Adb) -> str:
    return adb.shell("getprop", "persist.sys.locale").strip() or adb.shell("getprop", "ro.product.locale").strip()


def set_locale(adb: Adb, locale: str) -> None:
    """Switch the system locale (requires a google_apis image so `adb root` works)."""
    if current_locale(adb) == locale:
        return
    log(f"Switching emulator locale to {locale} (restarts the runtime)")
    adb("root")
    time.sleep(2)
    adb.shell("setprop", "persist.sys.locale", locale)
    adb.shell("setprop", "ctl.restart", "zygote")
    time.sleep(5)
    wait_until(lambda: adb.shell("getprop", "sys.boot_completed", check=False).strip() == "1",
               timeout=180, interval=3, what="runtime restart after locale change")
    time.sleep(3)
    prepare(adb)
