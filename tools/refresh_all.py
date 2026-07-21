#!/usr/bin/env python3
"""Re-render every diagram referenced from a Markdown file.

For each diagram reference (an image that lives in a folder containing ``.puml``
sources) ``pumlcli`` is run again on the ``.puml`` source, refreshing the
referenced output in its current format. The Markdown file itself is left
unchanged. References without a matching ``.puml`` source produce a warning.
"""
import argparse
import sys
from pathlib import Path

import puml_common as pc


def refresh(markdown_file: str) -> None:
    md_path: Path = Path(markdown_file).resolve()
    if not md_path.is_file():
        raise FileNotFoundError(f"Markdown file not found: {markdown_file}")

    md_dir: Path = md_path.parent
    text: str = md_path.read_text(encoding="utf-8")

    refreshed: int = 0
    for ref in pc.iter_image_refs(text, md_dir):
        ext: str = ref.target.suffix.lstrip(".").lower()
        if ext not in pc.RENDER_TYPES or not pc.dir_has_puml(ref.target.parent):
            continue

        puml_file: Path = ref.target.with_suffix(".puml")
        if not puml_file.is_file():
            print(f"⚠️  No .puml source for {ref.path} - skipping")
            continue

        if pc.run_pumlcli(puml_file, ref.target):
            refreshed += 1

    print(f"✅ Refreshed {refreshed} diagram(s)")


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Re-render all diagrams referenced from a Markdown file."
    )
    parser.add_argument("markdown_file", help="Markdown file to process")
    args = parser.parse_args()

    try:
        refresh(args.markdown_file)
    except Exception as exc:  # noqa: BLE001 - surface a clean message on the CLI
        print(f"Error: {exc}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
