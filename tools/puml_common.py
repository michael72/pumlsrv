#!/usr/bin/env python3
"""Shared helpers for the PlantUML-in-Markdown tooling.

These utilities are used by ``extract_puml.py``, ``convert_rendered.py`` and
``refresh_all.py`` to locate ``pumlcli``, render diagrams, discover diagram
references inside Markdown files and build the file names that follow the
``<markdown_name>_<index>[_<diagram_name>].<suffix>`` schema.
"""
from typing import Iterator, NamedTuple, Optional
import os
import re
import shutil
import subprocess
from pathlib import Path
from urllib.parse import unquote

# Output types understood by pumlcli / this tooling.
RENDER_TYPES: tuple[str, ...] = ("svg", "png", "txt")

# A ```plantuml``` / ```puml``` fenced code block. The closing fence must use
# the same indentation as the opening one.
FENCE_RE: re.Pattern[str] = re.compile(
    r"(?P<indent>[ \t]*)```[ \t]*(?P<lang>plantuml|puml)[^\n]*\n"
    r"(?P<body>.*?)\n(?P=indent)```[ \t]*(?=\n|$)",
    re.IGNORECASE | re.DOTALL,
)

# Any triple-backtick fenced code block - used to ignore image syntax that
# happens to live inside unrelated code blocks.
ANY_FENCE_RE: re.Pattern[str] = re.compile(
    r"(?P<indent>[ \t]*)```.*?\n.*?\n(?P=indent)```[ \t]*(?=\n|$)",
    re.DOTALL,
)

# A Markdown image reference: ![alt](path "optional title").
IMAGE_RE: re.Pattern[str] = re.compile(
    r"!\[(?P<alt>[^\]]*)\]\((?P<path>[^)\s]+)(?P<title>\s+\"[^\"]*\")?\)"
)


def find_pumlcli() -> str:
    """Return the path to the ``pumlcli`` executable.

    Prefers a ``pumlcli`` on ``PATH`` and falls back to the copy that ships
    next to these scripts. Raises ``FileNotFoundError`` if neither exists.
    """
    found: Optional[str] = shutil.which("pumlcli")
    if found:
        return found
    sibling: Path = Path(__file__).resolve().parent / "pumlcli"
    if sibling.is_file():
        return str(sibling)
    raise FileNotFoundError(
        "pumlcli not found on PATH or next to the script - is pumlsrv installed?"
    )


def run_pumlcli(puml_file: Path, out_file: Path) -> bool:
    """Render ``puml_file`` to ``out_file`` via ``pumlcli`` (type from extension).

    Returns True on success. On failure a warning is printed and False is
    returned so callers can continue with the remaining diagrams.
    """
    cmd: list[str] = [find_pumlcli(), "-f", str(puml_file), "-o", str(out_file)]
    try:
        subprocess.run(cmd, check=True)
        return True
    except subprocess.CalledProcessError as exc:
        print(f"⚠️  pumlcli failed for {puml_file.name} (exit {exc.returncode})")
        return False


def to_snake_case(name: str) -> str:
    """Convert an arbitrary label to snake_case suitable for a file name."""
    name = name.strip().strip("\"'")
    # spaces, dots and dashes become underscores
    name = re.sub(r"[\s.\-]+", "_", name)
    # split camelCase / PascalCase boundaries
    name = re.sub(r"(?<=[a-z0-9])(?=[A-Z])", "_", name)
    name = re.sub(r"(?<=[A-Z])(?=[A-Z][a-z])", "_", name)
    # drop anything that is not a word character or underscore
    name = re.sub(r"[^\w]", "", name)
    name = re.sub(r"_+", "_", name)
    return name.lower().strip("_")


def diagram_name_from_source(source: str) -> Optional[str]:
    """Extract the diagram name from a PlantUML ``@start...`` directive.

    Returns the snake_cased name, or None when the diagram is unnamed.
    """
    for line in source.splitlines():
        match = re.match(r"\s*@start\w+\b(?P<name>.*)$", line, re.IGNORECASE)
        if match:
            name: str = match.group("name").strip()
            if not name:
                return None
            snake: str = to_snake_case(name)
            return snake or None
    return None


def parse_stem(stem: str) -> tuple[Optional[str], Optional[str]]:
    """Split a schema stem into ``(index, diagram_name)``.

    ``readme_01_overview`` -> ``("01", "overview")``; ``readme_00`` ->
    ``("00", None)``. Returns ``(None, None)`` if no numeric index is present.
    """
    match = re.match(r"^.*?_(?P<idx>\d+)(?:_(?P<name>.+))?$", stem)
    if match:
        return match.group("idx"), match.group("name")
    return None, None


def build_stem(md_stem: str, index: int, diagram_name: Optional[str]) -> str:
    """Assemble a file stem following the naming schema."""
    stem: str = f"{md_stem}_{index:02d}"
    if diagram_name:
        stem += f"_{diagram_name}"
    return stem


def rel_ref(target: Path, start_dir: Path) -> str:
    """Return ``target`` relative to ``start_dir`` using forward slashes."""
    return os.path.relpath(target, start_dir).replace(os.sep, "/")


def resolve_ref(path: str, md_dir: Path) -> Path:
    """Resolve a Markdown reference path (relative to the Markdown file)."""
    raw: str = unquote(path)
    candidate: Path = Path(raw)
    if not candidate.is_absolute():
        candidate = md_dir / candidate
    return candidate.resolve()


def fenced_spans(text: str) -> list[tuple[int, int]]:
    """Return (start, end) spans of every triple-backtick fenced block."""
    return [(m.start(), m.end()) for m in ANY_FENCE_RE.finditer(text)]


def _inside_any(pos: int, spans: list[tuple[int, int]]) -> bool:
    return any(start <= pos < end for start, end in spans)


class ImageRef(NamedTuple):
    """A Markdown image reference located in a document."""

    start: int
    end: int
    alt: str
    path: str
    title: str  # includes leading whitespace + quotes, or ""
    target: Path  # resolved absolute path


def iter_image_refs(text: str, md_dir: Path) -> Iterator[ImageRef]:
    """Yield image references that are not inside unrelated code fences."""
    code_spans: list[tuple[int, int]] = fenced_spans(text)
    for m in IMAGE_RE.finditer(text):
        if _inside_any(m.start(), code_spans):
            continue
        path: str = m.group("path")
        # skip remote references
        if re.match(r"^[a-zA-Z][a-zA-Z0-9+.\-]*://", path) or path.startswith("data:"):
            continue
        yield ImageRef(
            start=m.start(),
            end=m.end(),
            alt=m.group("alt"),
            path=path,
            title=m.group("title") or "",
            target=resolve_ref(path, md_dir),
        )


_puml_dir_cache: dict[Path, bool] = {}


def dir_has_puml(directory: Path) -> bool:
    """True if ``directory`` contains at least one ``.puml`` file (cached)."""
    cached: Optional[bool] = _puml_dir_cache.get(directory)
    if cached is None:
        cached = directory.is_dir() and any(directory.glob("*.puml"))
        _puml_dir_cache[directory] = cached
    return cached
