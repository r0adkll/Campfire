"""The TOML sections every harness user shares: [server], [sample_library], [fixture], [app].

Each tool keeps its own spec file (tools/screenshots/shots.toml, tools/testbed/testbed.toml) and
adds its own sections on top; this module only parses the common ones.
"""
import os
from dataclasses import dataclass, field
from pathlib import Path

from .proc import HarnessError

REPO_ROOT = Path(__file__).resolve().parents[3]


def expand_path(value: str) -> Path:
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
class FixtureSpec:
    """The known server state a run starts from: Sample Library libraries plus seeded user data."""
    sample_library_path: Path
    regenerate_cmd: str | None
    libraries: list[LibraryDef]
    progress: list[ProgressSeed] = field(default_factory=list)
    sessions: list[SessionSeed] = field(default_factory=list)
    playlists: list[PlaylistSeed] = field(default_factory=list)
    match_authors: bool = False
    author_region: str = "us"


@dataclass
class AppConfig:
    variant: str
    package: str
    activity: str
    library: str
    theme_mode: str | None = None
    theme: str | None = None
    raw: dict = field(repr=False, default_factory=dict)


@dataclass
class DeviceDef:
    """One pinned emulator definition. The AVD is created under `avd_name` and never touches
    personal AVDs."""
    avd_name: str
    system_image: str
    width: int
    height: int
    density: int
    orientation: str


def _require(raw: dict, section: str) -> dict:
    if section not in raw:
        raise HarnessError(f"Spec is missing the [{section}] section")
    return raw[section]


def load_server(raw: dict) -> ServerConfig:
    s = _require(raw, "server")
    return ServerConfig(
        path=expand_path(s["path"]), repo=s["repo"], tag=s["tag"], port=int(s["port"]),
        username=s["username"], password=s["password"], server_name=s["server_name"],
        node=s.get("node", "auto"),
    )


def load_fixture(raw: dict) -> FixtureSpec:
    sl = _require(raw, "sample_library")
    sample_path = expand_path(sl["path"])
    fx = raw.get("fixture", {})
    return FixtureSpec(
        sample_library_path=sample_path,
        regenerate_cmd=sl.get("regenerate"),
        libraries=[
            LibraryDef(name=l["name"], folder=sample_path / l["folder"], media_type=l["media_type"])
            for l in sl.get("libraries", [])
        ],
        progress=[
            ProgressSeed(title=p["title"], progress=p.get("progress"), finished=bool(p.get("finished", False)))
            for p in fx.get("progress", [])
        ],
        sessions=[
            SessionSeed(title=x["title"], minutes=int(x["minutes"]), days_ago=int(x.get("days_ago", 0)))
            for x in fx.get("sessions", [])
        ],
        playlists=[
            PlaylistSeed(name=x["name"], description=x.get("description", ""), titles=list(x["titles"]))
            for x in fx.get("playlists", [])
        ],
        match_authors=bool(fx.get("match_authors", False)),
        author_region=fx.get("author_region", "us"),
    )


def load_app(raw: dict) -> AppConfig:
    a = _require(raw, "app")
    return AppConfig(
        variant=a["variant"], package=a["package"],
        activity=a.get("activity", "app.campfire.android.MainActivity"), library=a["library"],
        theme_mode=a.get("theme_mode"), theme=a.get("theme"), raw=a,
    )


def load_device(values: dict, avd_name: str) -> DeviceDef:
    return DeviceDef(
        avd_name=avd_name, system_image=values["system_image"], width=int(values["width"]),
        height=int(values["height"]), density=int(values["density"]), orientation=values["orientation"],
    )
