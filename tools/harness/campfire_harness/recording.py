"""Record an emulator's screen across separate processes, and turn recordings into reviewable frames.

`screenrecord` stops itself after 180 seconds, so the device runs it in a loop of segments that
`stop` joins back into one video. The loop is detached on the device (nohup), so it survives the
process that started it — `start` and `stop` can be two separate tool invocations.
"""
import re
import shutil
import tempfile
import time
from pathlib import Path

from .emulator import Adb
from .proc import HarnessError, log, run, wait_until

DEVICE_DIR = "/sdcard/campfire-recording"
STOP_FLAG = f"{DEVICE_DIR}/stop"
SEGMENT_SECONDS = 180


def is_recording(adb: Adb) -> bool:
    return bool(adb.shell("pgrep", "-x", "screenrecord", check=False).strip())


def start(adb: Adb, *, bit_rate: int = 4_000_000) -> None:
    if is_recording(adb):
        raise HarnessError("A screen recording is already running on this device; stop it first")
    adb.shell("rm", "-rf", DEVICE_DIR)
    adb.shell("mkdir", "-p", DEVICE_DIR)
    loop = (
        f"i=100; while [ ! -f {STOP_FLAG} ]; do "
        f"screenrecord --time-limit {SEGMENT_SECONDS} --bit-rate {bit_rate} {DEVICE_DIR}/seg_$i.mp4; "
        f"i=$((i+1)); done"
    )
    # Passed as one string (not through Adb.shell's quoting) so the device shell parses the
    # backgrounding and redirections itself.
    adb("shell", f"nohup sh -c '{loop}' >/dev/null 2>&1 &")
    wait_until(lambda: is_recording(adb), timeout=10, interval=0.5, what="screenrecord to start")


def stop(adb: Adb, dest: Path) -> Path | None:
    """End the recording and save it to `dest` (.mp4). Returns None if nothing was recorded."""
    adb.shell("touch", STOP_FLAG, check=False)
    # SIGINT lets screenrecord finish the mp4 container; a killed one leaves an unplayable file.
    adb.shell("pkill", "-2", "-x", "screenrecord", check=False)
    wait_until(lambda: not is_recording(adb), timeout=15, interval=0.5, what="screenrecord to finish")
    time.sleep(0.5)
    segments = sorted(adb.shell("ls", DEVICE_DIR, check=False).split())
    segments = [s for s in segments if s.startswith("seg_") and s.endswith(".mp4")]
    if not segments:
        return None
    dest.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory() as tmp:
        local = []
        for seg in segments:
            path = Path(tmp) / seg
            adb("pull", f"{DEVICE_DIR}/{seg}", str(path), timeout=300)
            if path.stat().st_size > 0:
                local.append(path)
        if len(local) == 1:
            shutil.move(local[0], dest)
        elif local:
            ffmpeg = _ffmpeg()
            if not ffmpeg:
                # Without ffmpeg, keep the segments side by side rather than lose any.
                for i, path in enumerate(local):
                    shutil.move(path, dest.with_name(f"{dest.stem}-{i}{dest.suffix}"))
                log("ffmpeg not found; kept the recording as separate segments")
                dest = dest.with_name(f"{dest.stem}-0{dest.suffix}")
            else:
                listing = Path(tmp) / "segments.txt"
                listing.write_text("".join(f"file '{p}'\n" for p in local))
                run([ffmpeg, "-hide_banner", "-loglevel", "error", "-y", "-f", "concat", "-safe", "0",
                     "-i", str(listing), "-c", "copy", str(dest)], capture=True)
    adb.shell("rm", "-rf", DEVICE_DIR, check=False)
    return dest if dest.exists() else None


def previews(video: Path, *, max_frames: int = 12) -> dict:
    """Extract the frames where the screen changed and tile them into a contact sheet, so the
    recording can be reviewed as images. Returns their paths plus each sheet frame's timestamp."""
    ffmpeg = _ffmpeg()
    if not ffmpeg:
        log("ffmpeg not found; skipping the frames and contact sheet")
        return {}
    result = {}

    # Every distinct screen state: mpdecimate drops a frame only when no 8x8 block changed much from
    # the last kept one, so a single label appearing still counts as a change.
    frames_dir = video.with_name(f"{video.stem}-frames")
    shutil.rmtree(frames_dir, ignore_errors=True)
    frames_dir.mkdir()
    proc = run([ffmpeg, "-hide_banner", "-i", str(video), "-vf", "scale=360:-2,mpdecimate,showinfo",
                "-fps_mode", "vfr", str(frames_dir / "%04d.png")], capture=True)
    times = [float(t) for t in re.findall(r"pts_time:([\d.]+)", proc.stderr.decode(errors="replace"))]
    frames = list(zip(sorted(frames_dir.glob("*.png")), times))
    kept = _spread(frames, max_frames * 4)
    kept_paths = {frame for frame, _ in kept}
    for frame, _ in frames:
        if frame not in kept_paths:
            frame.unlink()
    named = []
    for frame, t in kept:
        target = frame.with_name(f"{t:07.2f}s.png")
        frame.rename(target)
        named.append((target, t))
    if not named:
        return result
    result["frames_dir"] = str(frames_dir)

    sheet_frames = _spread(named, max_frames)
    cols = min(len(sheet_frames), 6)
    rows = -(-len(sheet_frames) // cols)
    listing = frames_dir / ".sheet.txt"
    listing.write_text("".join(f"file '{f}'\n" for f, _ in sheet_frames))
    sheet = video.with_name(f"{video.stem}-sheet.png")
    run([ffmpeg, "-hide_banner", "-loglevel", "error", "-y", "-f", "concat", "-safe", "0", "-i", str(listing),
         "-vf", f"tile={cols}x{rows}:padding=8:color=white", "-frames:v", "1", str(sheet)], capture=True)
    listing.unlink()
    result["sheet"] = str(sheet)
    result["sheet_frames_seconds"] = [round(t, 2) for _, t in sheet_frames]
    return result


def _spread(items: list, limit: int) -> list:
    """At most `limit` items, evenly spaced, always keeping the first and last."""
    if len(items) <= limit:
        return items
    return [items[round(i * (len(items) - 1) / (limit - 1))] for i in range(limit)]


def _ffmpeg() -> str | None:
    return shutil.which("ffmpeg") or next(
        (p for p in ("/opt/homebrew/bin/ffmpeg", "/usr/local/bin/ffmpeg") if Path(p).exists()), None)

