"""Write captured Shots into Store Metadata."""
import shutil
import struct
import zlib
from pathlib import Path

from .config import DeviceClass, Spec
from .proc import ShotError, log


def class_dir(spec: Spec, cls: DeviceClass, locale: str) -> Path:
    return spec.output_root / locale / "images" / cls.store_dir


def write_shots(spec: Spec, cls: DeviceClass, locale: str, captured: list[tuple[str, Path]],
                *, replace_all: bool, crop_9_16: bool) -> list[Path]:
    """captured: (shot name, png path) in spec order. Filenames are NN_Name.png by spec order."""
    dest_dir = class_dir(spec, cls, locale)
    dest_dir.mkdir(parents=True, exist_ok=True)
    order = {s.name: i + 1 for i, s in enumerate(s for s in spec.shots if cls.key in s.classes)}

    if replace_all:
        for old in dest_dir.glob("*.png"):
            old.unlink()

    written = []
    for name, src in captured:
        dest = dest_dir / f"{order[name]:02d}_{name}.png"
        # A partial run may replace a file whose number changed; drop stale siblings with this name.
        for stale in dest_dir.glob(f"*_{name}.png"):
            if stale != dest:
                stale.unlink()
        shutil.copyfile(src, dest)
        if crop_9_16:
            _crop_9_16(dest, cls)
        written.append(dest)
        log(f"Wrote {dest.relative_to(spec.output_root.parent.parent)}")
    return written


def _crop_9_16(path: Path, cls: DeviceClass) -> None:
    """Opt-in, top-anchored crop to 9:16 (see README "Design decisions"). Pure Python: keeps the first N
    scanlines of the PNG, which stay valid because PNG row filters only reference earlier rows."""
    data = path.read_bytes()
    if not data.startswith(b"\x89PNG\r\n\x1a\n"):
        raise ShotError(f"{path} is not a PNG")
    pos, ihdr, idat, trailing = 8, None, b"", []
    while pos < len(data):
        length = struct.unpack(">I", data[pos:pos + 4])[0]
        kind, body = data[pos + 4:pos + 8], data[pos + 8:pos + 8 + length]
        if kind == b"IHDR":
            ihdr = body
        elif kind == b"IDAT":
            idat += body
        elif kind not in (b"IEND",):
            trailing.append((kind, body))
        pos += 12 + length
    width, height = struct.unpack(">II", ihdr[:8])
    depth, color_type, interlace = ihdr[8], ihdr[9], ihdr[12]
    channels = {0: 1, 2: 3, 4: 2, 6: 4}.get(color_type)
    if channels is None or depth != 8 or interlace != 0:
        raise ShotError("9:16 crop supports only 8-bit non-interlaced RGB/RGBA PNGs")
    target_h = width * 9 // 16 if cls.orientation == "landscape" else width * 16 // 9
    if target_h > height:
        raise ShotError(f"Cannot crop {width}x{height} to 9:16 without padding")
    stride = 1 + width * channels
    raw = zlib.decompress(idat)[: stride * target_h]

    def chunk(kind: bytes, body: bytes) -> bytes:
        return struct.pack(">I", len(body)) + kind + body + struct.pack(">I", zlib.crc32(kind + body) & 0xFFFFFFFF)

    out = data[:8] + chunk(b"IHDR", struct.pack(">II", width, target_h) + ihdr[8:])
    out += b"".join(chunk(k, b) for k, b in trailing if k in (b"sRGB", b"gAMA", b"pHYs", b"iCCP"))
    out += chunk(b"IDAT", zlib.compress(raw, 6)) + chunk(b"IEND", b"")
    path.write_bytes(out)
