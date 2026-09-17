import sys
import tomllib
from datetime import datetime
from dataclasses import dataclass, field
from pathlib import Path

TOOL_DIR = Path(__file__).resolve().parents[1]
WORK_DIR = TOOL_DIR / ".work"
sys.path.insert(0, str(TOOL_DIR.parent / "harness"))

from campfire_harness.config import (  # noqa: E402
    REPO_ROOT,
    AppConfig,
    DeviceDef,
    FixtureSpec,
    ServerConfig,
    load_app,
    load_device,
    load_fixture,
    load_server,
)
from campfire_harness.proc import HarnessError  # noqa: E402


def pinned_now() -> datetime:
    """The instant every run pretends it is: today at 12:00 local. The emulator clock is pinned to it
    and Fixture timestamps (listening sessions) are seeded relative to it, so "today" on the device
    and "today" in the data always agree."""
    return datetime.now().replace(hour=12, minute=0, second=0, microsecond=0)


@dataclass
class DeviceClass:
    key: str
    store_dir: str
    device: DeviceDef


@dataclass
class Shot:
    name: str
    classes: list[str]
    steps: list[dict]
    enabled: bool = True
    library: str | None = None
    theme_mode: str | None = None
    theme: str | None = None
    settle_ms: int | None = None


@dataclass
class Spec:
    server: ServerConfig
    fixture: FixtureSpec
    app: AppConfig
    output_root: Path
    classes: dict[str, DeviceClass]
    shots: list[Shot]
    raw: dict = field(repr=False, default_factory=dict)

    def shots_for(self, class_key: str, names: list[str] | None) -> list[Shot]:
        chosen = []
        for shot in self.shots:
            if class_key not in shot.classes:
                continue
            if names is not None:
                if shot.name not in names:
                    continue
            elif not shot.enabled:
                continue
            chosen.append(shot)
        if names is not None:
            missing = set(names) - {s.name for s in chosen}
            if missing:
                raise HarnessError(
                    f"No shot named {sorted(missing)} for class '{class_key}'. "
                    f"Known: {[s.name for s in self.shots if class_key in s.classes]}"
                )
        return chosen


def load_spec(path: Path) -> Spec:
    with open(path, "rb") as f:
        raw = tomllib.load(f)

    classes = {
        key: DeviceClass(key=key, store_dir=c["store_dir"], device=load_device(c, avd_name=f"campfire-shots-{key}"))
        for key, c in raw["classes"].items()
    }
    shots = []
    for sh in raw.get("shot", []):
        unknown = set(sh["classes"]) - set(classes)
        if unknown:
            raise HarnessError(f"Shot '{sh['name']}' references unknown classes {sorted(unknown)}")
        shots.append(Shot(
            name=sh["name"], classes=list(sh["classes"]), steps=list(sh.get("steps", [])),
            enabled=bool(sh.get("enabled", True)), library=sh.get("library"),
            theme_mode=sh.get("theme_mode"), theme=sh.get("theme"), settle_ms=sh.get("settle_ms"),
        ))
    return Spec(
        server=load_server(raw), fixture=load_fixture(raw), app=load_app(raw),
        output_root=(REPO_ROOT / raw["output"]["root"]).resolve(), classes=classes, shots=shots, raw=raw,
    )
