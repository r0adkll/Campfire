import os
import tomllib
from dataclasses import dataclass, field
from pathlib import Path

from .proc import ShotError

REPO_ROOT = Path(__file__).resolve().parents[3]
TOOL_DIR = Path(__file__).resolve().parents[1]
WORK_DIR = TOOL_DIR / ".work"


def _p(value: str) -> Path:
    return Path(os.path.expanduser(value)).resolve()


@dataclass
class ServerConfig:
    path: Path
    repo: str
    tag: str
    port: int
    username: str
    password: str
    server_name: str
    node: str = "auto"

    @property
    def url_for_emulator(self) -> str:
        return f"http://10.0.2.2:{self.port}"

    @property
    def url_for_host(self) -> str:
        return f"http://127.0.0.1:{self.port}"


@dataclass
class LibraryDef:
    name: str
    folder: Path
    media_type: str


@dataclass
class ProgressSeed:
    title: str
    progress: float | None = None
    finished: bool = False


@dataclass
class SessionSeed:
    title: str
    minutes: int
    days_ago: int


@dataclass
class PlaylistSeed:
    name: str
    description: str
    titles: list[str]


@dataclass
class DeviceClass:
    key: str
    store_dir: str
    system_image: str
    width: int
    height: int
    density: int
    orientation: str

    @property
    def avd_name(self) -> str:
        return f"campfire-shots-{self.key}"


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
    sample_library_path: Path
    regenerate_cmd: str | None
    libraries: list[LibraryDef]
    progress: list[ProgressSeed]
    sessions: list[SessionSeed]
    playlists: list[PlaylistSeed]
    match_authors: bool
    author_region: str
    app: dict
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
                raise ShotError(
                    f"No shot named {sorted(missing)} for class '{class_key}'. "
                    f"Known: {[s.name for s in self.shots if class_key in s.classes]}"
                )
        return chosen


def load_spec(path: Path) -> Spec:
    with open(path, "rb") as f:
        raw = tomllib.load(f)

    s = raw["server"]
    server = ServerConfig(
        path=_p(s["path"]), repo=s["repo"], tag=s["tag"], port=int(s["port"]),
        username=s["username"], password=s["password"], server_name=s["server_name"],
        node=s.get("node", "auto"),
    )
    sl = raw["sample_library"]
    sample_path = _p(sl["path"])
    libraries = [
        LibraryDef(name=l["name"], folder=sample_path / l["folder"], media_type=l["media_type"])
        for l in sl.get("libraries", [])
    ]
    progress = [
        ProgressSeed(title=p["title"], progress=p.get("progress"), finished=bool(p.get("finished", False)))
        for p in raw.get("fixture", {}).get("progress", [])
    ]
    sessions = [
        SessionSeed(title=x["title"], minutes=int(x["minutes"]), days_ago=int(x.get("days_ago", 0)))
        for x in raw.get("fixture", {}).get("sessions", [])
    ]
    playlists = [
        PlaylistSeed(name=x["name"], description=x.get("description", ""), titles=list(x["titles"]))
        for x in raw.get("fixture", {}).get("playlists", [])
    ]
    classes = {
        key: DeviceClass(key=key, **{k: v for k, v in c.items()})
        for key, c in raw["classes"].items()
    }
    shots = []
    for sh in raw.get("shot", []):
        unknown = set(sh["classes"]) - set(classes)
        if unknown:
            raise ShotError(f"Shot '{sh['name']}' references unknown classes {sorted(unknown)}")
        shots.append(Shot(
            name=sh["name"], classes=list(sh["classes"]), steps=list(sh.get("steps", [])),
            enabled=bool(sh.get("enabled", True)), library=sh.get("library"),
            theme_mode=sh.get("theme_mode"), theme=sh.get("theme"), settle_ms=sh.get("settle_ms"),
        ))
    return Spec(
        server=server, sample_library_path=sample_path, regenerate_cmd=sl.get("regenerate"),
        libraries=libraries, progress=progress, sessions=sessions, playlists=playlists,
        match_authors=bool(raw.get("fixture", {}).get("match_authors", False)),
        author_region=raw.get("fixture", {}).get("author_region", "us"), app=raw["app"],
        output_root=(REPO_ROOT / raw["output"]["root"]).resolve(), classes=classes, shots=shots, raw=raw,
    )
