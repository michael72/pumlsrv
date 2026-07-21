#!/usr/bin/env python3
"""Extract PlantUML blocks from a Markdown file into standalone diagram files.

Every ```plantuml``` / ```puml``` fenced block is written to the diagrams
folder as ``<markdown_name>_<index>[_<diagram_name>].puml`` and rendered via
``pumlcli`` to the requested output type (svg by default). The fenced block in
the Markdown file is replaced by an image reference to the rendered output.

Diagram files that are already referenced from the Markdown file are re-indexed
in document order: their ``.puml`` source and every rendered output are renamed
to match the naming schema and the references are updated accordingly.
"""
from typing import NamedTuple, Optional
import argparse
import sys
from pathlib import Path

import puml_common as pc


class _Item(NamedTuple):
    start: int
    end: int
    kind: str  # "fence" or "image"
    # fence:
    indent: str
    body: str
    # image:
    old_stem: str
    ext: str
    alt: str
    title: str


def _plan_items(text: str, md_dir: Path, diagrams_dir: Path) -> list[_Item]:
    """Collect fenced puml blocks and existing diagram references, in order."""
    items: list[_Item] = []

    for m in pc.FENCE_RE.finditer(text):
        items.append(
            _Item(
                start=m.start(),
                end=m.end(),
                kind="fence",
                indent=m.group("indent"),
                body=m.group("body"),
                old_stem="",
                ext="",
                alt="",
                title="",
            )
        )

    for ref in pc.iter_image_refs(text, md_dir):
        if ref.target.parent != diagrams_dir:
            continue
        if ref.target.suffix.lstrip(".").lower() not in pc.RENDER_TYPES:
            continue
        items.append(
            _Item(
                start=ref.start,
                end=ref.end,
                kind="image",
                indent="",
                body="",
                old_stem=ref.target.stem,
                ext=ref.target.suffix,
                alt=ref.alt,
                title=ref.title,
            )
        )

    items.sort(key=lambda it: it.start)
    return items


def _two_phase_rename(diagrams_dir: Path, renames: list[tuple[str, str]]) -> None:
    """Rename ``old_stem.*`` to ``new_stem.*`` for each pair, collision-safe."""
    staged: list[tuple[Path, Path]] = []
    for i, (old_stem, _new_stem) in enumerate(renames):
        for src in sorted(diagrams_dir.glob(f"{old_stem}.*")):
            tmp: Path = src.with_name(f".__reindex_{i}__{src.name}")
            src.rename(tmp)
            staged.append((tmp, src))

    stem_map: dict[str, str] = dict(renames)
    for tmp, original in staged:
        new_stem: str = stem_map[original.stem]
        dest: Path = tmp.with_name(f"{new_stem}{original.suffix}")
        tmp.rename(dest)


def extract(markdown_file: str, diagrams_folder: str, render_type: str) -> None:
    md_path: Path = Path(markdown_file).resolve()
    if not md_path.is_file():
        raise FileNotFoundError(f"Markdown file not found: {markdown_file}")

    md_dir: Path = md_path.parent
    md_stem: str = pc.to_snake_case(md_path.stem)

    folder_path: Path = Path(diagrams_folder)
    diagrams_dir: Path = (
        folder_path if folder_path.is_absolute() else md_dir / folder_path
    ).resolve()
    diagrams_dir.mkdir(parents=True, exist_ok=True)

    text: str = md_path.read_text(encoding="utf-8")
    items: list[_Item] = _plan_items(text, md_dir, diagrams_dir)

    if not items:
        print("No PlantUML blocks or diagram references found - nothing to do.")
        return

    parts: list[str] = []
    last: int = 0
    renames: list[tuple[str, str]] = []
    new_diagrams: list[tuple[Path, str]] = []  # (puml_file, body)
    extracted: int = 0
    reindexed: int = 0

    for index, item in enumerate(items):
        parts.append(text[last:item.start])

        if item.kind == "fence":
            name: Optional[str] = pc.diagram_name_from_source(item.body)
            new_stem: str = pc.build_stem(md_stem, index, name)
            puml_file: Path = diagrams_dir / f"{new_stem}.puml"
            out_file: Path = diagrams_dir / f"{new_stem}.{render_type}"
            new_diagrams.append((puml_file, item.body))
            rel: str = pc.rel_ref(out_file, md_dir)
            alt: str = name or new_stem
            parts.append(f"{item.indent}![{alt}]({rel})")
            extracted += 1
        else:  # existing reference
            _, dname = pc.parse_stem(item.old_stem)
            new_stem = pc.build_stem(md_stem, index, dname)
            if new_stem != item.old_stem:
                renames.append((item.old_stem, new_stem))
                reindexed += 1
            new_ref: str = pc.rel_ref(diagrams_dir / f"{new_stem}{item.ext}", md_dir)
            parts.append(f"![{item.alt}]({new_ref}{item.title})")

        last = item.end

    parts.append(text[last:])
    new_text: str = "".join(parts)

    # 1) re-index existing files first so new writes cannot clobber them
    if renames:
        _two_phase_rename(diagrams_dir, renames)

    # 2) write and render the newly extracted diagrams
    for puml_file, body in new_diagrams:
        puml_file.write_text(body.rstrip("\n") + "\n", encoding="utf-8")
        out_file = puml_file.with_suffix(f".{render_type}")
        pc.run_pumlcli(puml_file, out_file)

    # 3) update the Markdown file
    md_path.write_text(new_text, encoding="utf-8")

    print(f"✅ Extracted {extracted} diagram(s) to {diagrams_dir}")
    if reindexed:
        print(f"🔁 Re-indexed {reindexed} existing reference(s)")


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Extract PlantUML blocks from a Markdown file into diagram files."
    )
    parser.add_argument("markdown_file", help="Markdown file to process")
    parser.add_argument(
        "diagrams_folder",
        nargs="?",
        default="diagrams",
        help="Folder for the extracted diagrams (default: diagrams)",
    )
    parser.add_argument(
        "-t",
        "--type",
        dest="render_type",
        choices=pc.RENDER_TYPES,
        default="svg",
        help="Render output type (default: svg)",
    )
    args = parser.parse_args()

    try:
        extract(args.markdown_file, args.diagrams_folder, args.render_type)
    except Exception as exc:  # noqa: BLE001 - surface a clean message on the CLI
        print(f"Error: {exc}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
