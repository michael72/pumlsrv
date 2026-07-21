#!/usr/bin/env python3
"""Convert diagram references in a Markdown file to a different output type.

For every diagram reference (an image that lives in a folder containing
``.puml`` sources) the referenced output is re-rendered to the requested type:

* references already in the target format are skipped;
* references without a matching ``.puml`` source produce a warning and are
  skipped;
* otherwise ``pumlcli`` renders the ``.puml`` source to the target type (same
  name and location) and the Markdown reference is updated.
"""
import argparse
import sys
from pathlib import Path

import puml_common as pc


def convert(markdown_file: str, render_type: str) -> None:
    md_path: Path = Path(markdown_file).resolve()
    if not md_path.is_file():
        raise FileNotFoundError(f"Markdown file not found: {markdown_file}")

    md_dir: Path = md_path.parent
    text: str = md_path.read_text(encoding="utf-8")

    parts: list[str] = []
    last: int = 0
    converted: int = 0

    for ref in pc.iter_image_refs(text, md_dir):
        ext: str = ref.target.suffix.lstrip(".").lower()
        # only render-type outputs sitting next to .puml sources are diagrams
        if ext not in pc.RENDER_TYPES or not pc.dir_has_puml(ref.target.parent):
            continue

        parts.append(text[last:ref.start])
        last = ref.end

        if ext == render_type:
            parts.append(text[ref.start:ref.end])  # already in target format
            continue

        puml_file: Path = ref.target.with_suffix(".puml")
        if not puml_file.is_file():
            print(f"⚠️  No .puml source for {ref.path} - skipping")
            parts.append(text[ref.start:ref.end])
            continue

        out_file: Path = puml_file.with_suffix(f".{render_type}")
        pc.run_pumlcli(puml_file, out_file)
        new_ref: str = pc.rel_ref(out_file, md_dir)
        parts.append(f"![{ref.alt}]({new_ref}{ref.title})")
        converted += 1

    parts.append(text[last:])

    if converted:
        md_path.write_text("".join(parts), encoding="utf-8")
    print(f"✅ Converted {converted} reference(s) to {render_type}")


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Convert Markdown diagram references to a different output type."
    )
    parser.add_argument("markdown_file", help="Markdown file to process")
    parser.add_argument(
        "-t",
        "--type",
        dest="render_type",
        choices=pc.RENDER_TYPES,
        default="svg",
        help="Target render type (default: svg)",
    )
    args = parser.parse_args()

    try:
        convert(args.markdown_file, args.render_type)
    except Exception as exc:  # noqa: BLE001 - surface a clean message on the CLI
        print(f"Error: {exc}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
