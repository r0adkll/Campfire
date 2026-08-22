"""Write captured Shots into Store Metadata."""
import shutil
from pathlib import Path

from .config import DeviceClass, Spec
from .proc import log


def class_dir(spec: Spec, cls: DeviceClass, locale: str) -> Path:
    return spec.output_root / locale / "images" / cls.store_dir


def write_shots(spec: Spec, cls: DeviceClass, locale: str, captured: list[tuple[str, Path]],
                *, replace_all: bool) -> list[Path]:
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
        written.append(dest)
        log(f"Wrote {dest.relative_to(spec.output_root.parent.parent)}")
    return written

