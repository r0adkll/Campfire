import os
import shutil
import subprocess
import sys
import time


class ShotError(Exception):
    """A failure the user needs to act on. Printed without a traceback."""


def log(msg: str) -> None:
    print(f"[shots] {msg}", file=sys.stderr, flush=True)


def run(cmd, *, check=True, capture=False, cwd=None, env=None, timeout=None, input_bytes=None):
    """Run a command. Returns CompletedProcess; stdout is text when capture=True."""
    kwargs = dict(cwd=cwd, env=env, timeout=timeout, input=input_bytes)
    if capture:
        kwargs.update(stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    result = subprocess.run(cmd, **kwargs)
    if check and result.returncode != 0:
        detail = ""
        if capture:
            detail = "\n" + (result.stderr or b"").decode(errors="replace").strip()
        raise ShotError(f"Command failed ({result.returncode}): {' '.join(map(str, cmd))}{detail}")
    return result


def out(cmd, **kwargs) -> str:
    return run(cmd, capture=True, **kwargs).stdout.decode(errors="replace")


def which(name: str, *candidates: str) -> str:
    found = shutil.which(name)
    if found:
        return found
    for c in candidates:
        c = os.path.expanduser(c)
        if os.path.exists(c):
            return c
    raise ShotError(f"'{name}' not found on PATH (also looked in {', '.join(candidates) or 'nowhere'})")


def wait_until(predicate, *, timeout: float, interval: float = 1.0, what: str = "condition"):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        if predicate():
            return True
        time.sleep(interval)
    raise ShotError(f"Timed out after {timeout:.0f}s waiting for {what}")
