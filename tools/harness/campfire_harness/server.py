"""Run a fresh local Audiobookshelf server and seed the Fixture."""
import json
import os
import signal
import uuid
import shutil
import subprocess
import time
from datetime import datetime
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

from .config import FixtureSpec, ServerConfig
from .proc import HarnessError, log, out, run, wait_until


class AbsClient:
    def __init__(self, base_url: str):
        self.base_url = base_url.rstrip("/")
        self.token: str | None = None

    def request(self, method: str, path: str, body: dict | None = None, *, auth=True, timeout=30):
        data = json.dumps(body).encode() if body is not None else None
        req = urllib.request.Request(self.base_url + path, data=data, method=method)
        req.add_header("Accept", "application/json")
        if data is not None:
            req.add_header("Content-Type", "application/json")
        if auth and self.token:
            req.add_header("Authorization", f"Bearer {self.token}")
        try:
            with urllib.request.urlopen(req, timeout=timeout) as resp:
                raw = resp.read()
        except urllib.error.HTTPError as e:
            raise HarnessError(f"{method} {path} -> HTTP {e.code}: {e.read().decode(errors='replace')[:300]}")
        if not raw:
            return None
        try:
            return json.loads(raw)
        except json.JSONDecodeError:
            return raw.decode(errors="replace")

    def status(self):
        return self.request("GET", "/status", auth=False, timeout=5)

    def init_root(self, username: str, password: str):
        self.request("POST", "/init", {"newRoot": {"username": username, "password": password}}, auth=False)

    def login(self, username: str, password: str):
        res = self.request("POST", "/login", {"username": username, "password": password}, auth=False)
        user = res["user"]
        self.token = user.get("accessToken") or user.get("token")
        if not self.token:
            raise HarnessError("Login succeeded but no token in response")
        return user

    def create_library(self, name: str, folder: Path, media_type: str) -> str:
        res = self.request("POST", "/api/libraries", {
            "name": name,
            "folders": [{"fullPath": str(folder)}],
            "mediaType": media_type,
            "provider": "google",
        })
        return res["id"]

    def libraries(self) -> list[dict]:
        return list((self.request("GET", "/api/libraries") or {}).get("libraries", []))

    def scan_library(self, library_id: str):
        self.request("POST", f"/api/libraries/{library_id}/scan?force=1")

    def running_scans(self) -> set[str]:
        res = self.request("GET", "/api/tasks") or {}
        return {
            t.get("data", {}).get("libraryId")
            for t in res.get("tasks", [])
            if t.get("action") == "library-scan"
        }

    def item_count(self, library_id: str) -> int:
        res = self.request("GET", f"/api/libraries/{library_id}/items?limit=1")
        return int(res.get("total", 0))

    def find_item(self, library_id: str, title: str) -> dict:
        q = urllib.parse.quote(title)
        res = self.request("GET", f"/api/libraries/{library_id}/search?q={q}&limit=25")
        candidates = [r["libraryItem"] for r in res.get("book", []) + res.get("podcast", [])]
        for item in candidates:
            if item.get("media", {}).get("metadata", {}).get("title", "").strip().lower() == title.strip().lower():
                return item
        if len(candidates) == 1:
            return candidates[0]
        names = [c.get("media", {}).get("metadata", {}).get("title") for c in candidates]
        raise HarnessError(f"No item titled '{title}' (search returned {names})")

    def sync_local_sessions(self, sessions: list[dict]):
        res = self.request("POST", "/api/session/local-all", {"sessions": sessions, "deviceInfo": {
            "deviceId": "campfire-shots", "clientName": "Campfire", "manufacturer": "Google", "model": "Pixel",
        }})
        failed = [r for r in (res or {}).get("results", []) if not r.get("success")]
        if failed:
            raise HarnessError(f"Session seeding failed: {failed}")

    def list_authors(self, library_id: str) -> list[dict]:
        res = self.request("GET", f"/api/libraries/{library_id}/authors") or {}
        return list(res.get("authors", []))

    def match_author(self, author_id: str, name: str, region: str) -> bool:
        """Quick-match an author against Audible so ABS saves a real author photo. Returns success."""
        try:
            res = self.request("POST", f"/api/authors/{author_id}/match", {"q": name, "region": region}, timeout=60)
        except HarnessError as e:
            log(f"  match failed for {name}: {e}")
            return False
        return bool((res or {}).get("author", {}).get("imagePath") or (res or {}).get("updated"))

    def create_playlist(self, library_id: str, name: str, description: str, item_ids: list[str]) -> str:
        res = self.request("POST", "/api/playlists", {
            "libraryId": library_id,
            "name": name,
            "description": description,
            "items": [{"libraryItemId": i} for i in item_ids],
        })
        return res["id"]

    def set_progress(self, item_id: str, duration: float, *, progress: float | None, finished: bool):
        body: dict = {"duration": duration}
        if finished:
            body.update({"isFinished": True, "progress": 1, "currentTime": duration})
        else:
            body.update({"isFinished": False, "progress": progress, "currentTime": round(duration * progress, 1)})
        self.request("PATCH", f"/api/me/progress/{item_id}", body)


class Server:
    """Lifecycle of the local ABS checkout: ensure → start (fresh data dir) → stop.

    The server runs in its own process group so it (and a `nix shell` wrapper around Node) can be
    stopped from a later process that only knows the pid — see `stop_pid`.
    """

    def __init__(self, cfg: ServerConfig, work_dir: Path):
        self.cfg = cfg
        self.proc: subprocess.Popen | None = None
        self.work_dir = work_dir
        self.data_dir = work_dir / "server"
        self.log_path = work_dir / "server.log"
        self.client = AbsClient(cfg.url_for_host)

    def ensure_checkout(self):
        path = self.cfg.path
        if not path.exists():
            log(f"No Audiobookshelf checkout at {path}; cloning {self.cfg.repo} @ {self.cfg.tag}")
            path.parent.mkdir(parents=True, exist_ok=True)
            run(["git", "clone", "--depth", "1", "--branch", self.cfg.tag, self.cfg.repo, str(path)])
        else:
            ref = out(["git", "-C", str(path), "describe", "--tags", "--always"]).strip()
            if ref != self.cfg.tag:
                log(f"WARNING: {path} is at '{ref}', spec pins '{self.cfg.tag}'. Continuing with the checkout as-is.")
        self.node = self._resolve_node()
        if not (path / "node_modules").exists():
            log("Installing server dependencies (npm ci)…")
            run([*self.node[:-1], "npm", "ci"] if len(self.node) > 1 else ["npm", "ci"], cwd=str(path))

    SUPPORTED_NODE = range(20, 23)

    def _resolve_node(self) -> list[str]:
        """Command prefix that runs a supported Node. [server].node overrides; 'auto' probes PATH then nix."""
        configured = self.cfg.node
        candidates = []
        if configured and configured != "auto":
            candidates.append(configured.split())
        else:
            candidates.append(["node"])
            if shutil.which("nix"):
                candidates.append(["nix", "shell", "nixpkgs#nodejs_22", "--command", "node"])
        for cmd in candidates:
            try:
                version = out([*cmd, "--version"], timeout=600).strip()
            except HarnessError:
                continue
            major = int(version.lstrip("v").split(".")[0])
            if major in self.SUPPORTED_NODE:
                log(f"Using Node {version} via: {' '.join(cmd)}")
                return cmd
            log(f"Node {version} from '{' '.join(cmd)}' is outside the supported range 20-22")
        raise HarnessError(
            "No supported Node (20-22) found. Install one, or set [server].node in shots.toml "
            "(e.g. node = \"/opt/homebrew/opt/node@22/bin/node\")"
        )

    def is_up(self) -> bool:
        return self._port_in_use()

    def _port_in_use(self) -> bool:
        try:
            self.client.status()
            return True
        except Exception:
            return False

    def start(self):
        if self._port_in_use():
            raise HarnessError(
                f"Something is already listening on port {self.cfg.port}; stop it or change [server].port"
            )
        if self.data_dir.exists():
            shutil.rmtree(self.data_dir)
        config_dir, metadata_dir = self.data_dir / "config", self.data_dir / "metadata"
        config_dir.mkdir(parents=True)
        metadata_dir.mkdir(parents=True)
        self.work_dir.mkdir(parents=True, exist_ok=True)
        env = dict(os.environ, PORT=str(self.cfg.port), HOST="0.0.0.0",
                   CONFIG_PATH=str(config_dir), METADATA_PATH=str(metadata_dir), NODE_ENV="production")
        log(f"Starting Audiobookshelf on :{self.cfg.port} (log: {self.log_path})")
        logf = open(self.log_path, "wb")
        self.proc = subprocess.Popen([*self.node, "index.js"], cwd=str(self.cfg.path), env=env,
                                     stdout=logf, stderr=subprocess.STDOUT, start_new_session=True)

        def up_or_dead():
            if self.proc.poll() is not None:
                tail = self.log_path.read_text(errors="replace").splitlines()[-15:]
                raise HarnessError("Audiobookshelf exited during startup:\n" + "\n".join(tail))
            return self._port_in_use()

        wait_until(up_or_dead, timeout=120, what="server to come up")

    def stop(self):
        if self.proc and self.proc.poll() is None:
            log("Stopping Audiobookshelf")
            self.stop_pid(self.proc.pid)
            try:
                self.proc.wait(timeout=5)
            except subprocess.TimeoutExpired:
                pass
        self.proc = None

    def stop_pid(self, pid: int):
        """Stop a server started earlier (possibly by another process) by its process group."""
        for sig in (signal.SIGTERM, signal.SIGKILL):
            try:
                os.killpg(pid, sig)
            except ProcessLookupError:
                return
            try:
                wait_until(lambda: not self._port_in_use(), timeout=15, interval=0.5, what="server to stop")
                return
            except HarnessError:
                continue


class Fixture:
    """Initialize the fresh server, create libraries, scan, and seed progress.

    `now` anchors seeded listening sessions ("n days ago"); the screenshot tool passes the instant
    its emulator clock is pinned to.
    """

    def __init__(self, spec: FixtureSpec, server: ServerConfig, client: AbsClient, *, now: datetime | None = None):
        self.spec = spec
        self.server = server
        self.client = client
        self.now = now or datetime.now()
        self.library_ids: dict[str, str] = {}

    def apply(self):
        cfg = self.server
        status = self.client.status()
        if status.get("isInit"):
            raise HarnessError("Server is already initialized; the data dir should have been fresh")
        self.client.init_root(cfg.username, cfg.password)
        self.client.login(cfg.username, cfg.password)

        for lib in self.spec.libraries:
            if not lib.folder.is_dir():
                raise HarnessError(f"Sample Library folder missing: {lib.folder}")
            self.library_ids[lib.name] = self.client.create_library(lib.name, lib.folder, lib.media_type)
            log(f"Created library '{lib.name}' → {lib.folder}")

        self.scan()

        # Listening sessions (drive the statistics screen). Seeded *before* progress so the
        # progress PATCH below is the newest write and wins for Continue Listening.
        if self.spec.sessions:
            now_ms = int(self.now.timestamp() * 1000)
            payload = []
            for seed in self.spec.sessions:
                item = self.find_book(seed.title)
                seconds = seed.minutes * 60
                ended = now_ms - seed.days_ago * 86_400_000 - 3_600_000
                metadata = item.get("media", {}).get("metadata", {})
                payload.append({
                    "id": str(uuid.uuid4()),
                    "libraryItemId": item["id"],
                    "mediaType": item.get("mediaType", "book"),
                    "displayTitle": metadata.get("title"),
                    "displayAuthor": metadata.get("authorName"),
                    "mediaPlayer": "exoplayer",
                    "timeListening": seconds,
                    "startTime": 0,
                    "currentTime": 0,
                    "playMethod": 1,
                    "startedAt": ended - seconds * 1000,
                    "updatedAt": ended,
                })
            self.client.sync_local_sessions(payload)
            log(f"Seeded {len(payload)} listening sessions ({sum(s.minutes for s in self.spec.sessions)} min)")

        if self.spec.match_authors:
            for name, lib_id in self.library_ids.items():
                authors = self.client.list_authors(lib_id)
                matched = sum(
                    self.client.match_author(a["id"], a["name"], self.spec.author_region)
                    for a in authors if not a.get("imagePath")
                )
                log(f"Matched author images in '{name}': {matched}/{len(authors)} (via Audible {self.spec.author_region})")

        for playlist in self.spec.playlists:
            items = [self.find_book(t) for t in playlist.titles]
            library_ids = {i["libraryId"] for i in items}
            if len(library_ids) != 1:
                raise HarnessError(f"Playlist '{playlist.name}' spans libraries {library_ids}; ABS playlists are per-library")
            self.client.create_playlist(library_ids.pop(), playlist.name, playlist.description, [i["id"] for i in items])
            log(f"Created playlist '{playlist.name}' ({len(items)} items)")

        for seed in self.spec.progress:
            item = self.find_book(seed.title)
            duration = float(item.get("media", {}).get("duration") or 0)
            if duration <= 0:
                raise HarnessError(f"'{seed.title}' has no duration; cannot seed progress")
            self.client.set_progress(item["id"], duration, progress=seed.progress, finished=seed.finished)
            log(f"Seeded progress: {seed.title} → {'finished' if seed.finished else f'{seed.progress:.0%}'}")

    def attach(self):
        """Pick up an already-applied Fixture on a running server: sign in and map library names to ids."""
        self.client.login(self.server.username, self.server.password)
        names = {lib.name for lib in self.spec.libraries}
        self.library_ids = {lib["name"]: lib["id"] for lib in self.client.libraries() if lib["name"] in names}

    def scan(self, library_ids: list[str] | None = None):
        """Scan libraries (default: every Fixture library) and block until the scans finish."""
        ids = library_ids or list(self.library_ids.values())
        for lib_id in ids:
            self.client.scan_library(lib_id)
        time.sleep(2)
        wait_until(lambda: not (self.client.running_scans() & set(ids)),
                   timeout=600, interval=3, what="library scans to finish")
        for name, lib_id in self.library_ids.items():
            if lib_id in ids:
                log(f"Scanned '{name}': {self.client.item_count(lib_id)} items")

    def find_book(self, title: str) -> dict:
        errors = []
        for lib_id in self.library_ids.values():
            try:
                return self.client.find_item(lib_id, title)
            except HarnessError as e:
                errors.append(str(e))
        raise HarnessError(f"Could not resolve '{title}' in any library: {errors}")
