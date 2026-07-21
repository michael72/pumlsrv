#!/usr/bin/env python3
"""Inline diagram sources back into a Markdown file (the inverse of extract).

For every diagram reference (an image that lives in a folder containing
``.puml`` sources) the matching ``.puml`` file is looked up and its content is
inlined into the Markdown file as a ` ```plantuml ` fenced block, replacing the
image reference:

* references without a matching ``.puml`` source produce a warning and are left
  untouched;
* by default the inlined diagram files (the ``.puml`` source and its rendered
  outputs) are removed; pass ``--keep`` / ``-k`` to retain them on disk.
"""
import argparse
import sys
from pathlib import Path

import puml_common as pc


def _fence(indent: str, body: str) -> str:
    """Wrap ``body`` in a ` ```plantuml ` block.

    The opening fence carries no indent of its own - it reuses the leading
    whitespace already present in front of the replaced image reference - while
    the body and closing fence are indented so the block stays aligned.
    """
    lines: list[str] = body.splitlines()
    inner: str = "\n".join(f"{indent}{line}".rstrip() for line in lines)
    return f"```plantuml\n{inner}\n{indent}```"


def _line_indent(text: str, pos: int) -> str:
    """Return the leading whitespace of the line that ``pos`` sits on."""
    line_start: int = text.rfind("\n", 0, pos) + 1
    prefix: str = text[line_start:pos]
    return prefix if prefix.strip() == "" else ""


def integrate(markdown_file: str, keep: bool = False) -> None:
    md_path: Path = Path(markdown_file).resolve()
    if not md_path.is_file():
        raise FileNotFoundError(f"Markdown file not found: {markdown_file}")

    md_dir: Path = md_path.parent
    text: str = md_path.read_text(encoding="utf-8")

    parts: list[str] = []
    last: int = 0
    integrated: int = 0
    # inlined sources, deduped - the same diagram may be referenced repeatedly
    inlined: set[Path] = set()

    for ref in pc.iter_image_refs(text, md_dir):
        ext: str = ref.target.suffix.lstrip(".").lower()
        # only render-type outputs sitting next to .puml sources are diagrams
        if ext not in pc.RENDER_TYPES or not pc.dir_has_puml(ref.target.parent):
            continue

        puml_file: Path = ref.target.with_suffix(".puml")
        if not puml_file.is_file():
            print(f"⚠️  No .puml source for {ref.path} - skipping")
            continue

        parts.append(text[last:ref.start])
        last = ref.end

        indent: str = _line_indent(text, ref.start)
        body: str = puml_file.read_text(encoding="utf-8").rstrip("\n")
        parts.append(_fence(indent, body))
        inlined.add(puml_file)
        integrated += 1

    parts.append(text[last:])

    if integrated:
        md_path.write_text("".join(parts), encoding="utf-8")

    removed: int = 0
    if integrated and not keep:
        # remove the inlined .puml source and every rendered output next to it
        for puml_file in inlined:
            for f in sorted(puml_file.parent.glob(f"{puml_file.stem}.*")):
                f.unlink()
                removed += 1

    print(f"✅ Integrated {integrated} diagram(s) into {md_path.name}")
    if removed:
        print(f"🗑️  Removed {removed} diagram file(s)")


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Inline diagram sources back into a Markdown file as "
        "```plantuml``` blocks (the inverse of extract_puml)."
    )
    parser.add_argument("markdown_file", help="Markdown file to process")
    parser.add_argument(
        "-k",
        "--keep",
        action="store_true",
        help="keep the inlined diagram files on disk (default: remove them)",
    )
    args = parser.parse_args()

    try:
        integrate(args.markdown_file, args.keep)
    except Exception as exc:  # noqa: BLE001 - surface a clean message on the CLI
        print(f"Error: {exc}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
