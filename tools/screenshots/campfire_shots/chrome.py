"""Store-screenshot styling on top of the harness emulator: status bar, pinned clock, no soft
keyboard, locale."""
import time

from campfire_harness import emulator
from campfire_harness.emulator import Adb
from campfire_harness.proc import log, wait_until

from .config import pinned_now


def prepare(adb: Adb) -> None:
    """Deterministic chrome for captures, on top of the harness's automation settings."""
    emulator.prepare(adb)
    # No soft keyboard in captures: the AVD has a hardware keyboard (hw.keyboard=yes) and the soft
    # keyboard is told not to show alongside it. `input text` injects key events directly.
    adb.shell("settings", "put", "secure", "show_ime_with_hard_keyboard", "0")

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
    # Hide the mobile signal/RAT icons: the "5G" label comes and goes between runs. Cosmetic only —
    # disabling mobile data instead breaks the emulator's route to the host.
    adb.shell("settings", "put", "secure", "icon_blacklist", "mobile", check=False)
    set_clock(adb)
    # Hide the "USB debugging connected" notification icon
    adb.shell("setprop", "persist.adb.notify", "0", check=False)
    adb.shell("pkill", "-f", "com.android.systemui", check=False)
    time.sleep(6)
    # Hide notification icons (ADB debugging etc.) from the freshly restarted bar
    adb.shell("cmd", "statusbar", "send-disable-flag", "notification-icons", check=False)


def set_clock(adb: Adb) -> None:
    """Pin the device clock to `pinned_now()` (needs root). Called before every capture so it never drifts."""
    adb.shell("date", pinned_now().strftime("%m%d%H%M%Y.%S"), check=False)
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
