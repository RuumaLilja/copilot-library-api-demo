from __future__ import annotations

import re
from pathlib import Path


BASE_DIR = Path("docs/ai-logs")
RAW_DIR = BASE_DIR / "raw"
PUBLIC_DIR = BASE_DIR / "public"


NOISE_LINE_PATTERNS: list[re.Pattern[str]] = [
    re.compile(r"^Created \d+ todos\s*$"),
    re.compile(r"^Starting:.*$"),
    re.compile(r"^Completed:.*$"),
    re.compile(r"^Made changes\.?\s*$"),
    re.compile(r"^.*file:///.*$"),
    re.compile(r"^パッチを生成中.*$"),
    re.compile(r"^Response cleared due to content safety filters.*$"),
]

ABS_PATH_PATTERNS: list[tuple[re.Pattern[str], str]] = [
    # Windows absolute paths
    (re.compile(r"[A-Za-z]:\\\\Users\\\\[^\\\\]+\\\\"), "<LOCAL_PATH>\\"),
    (re.compile(r"[A-Za-z]:/Users/[^/]+/"), "<LOCAL_PATH>/"),
    (re.compile(r"/c:/Users/[^/]+/"), "<LOCAL_PATH>/"),
]


def _is_noise_line(line: str) -> bool:
    return any(p.match(line) for p in NOISE_LINE_PATTERNS)


def _sanitize_text(text: str) -> str:
    for pattern, repl in ABS_PATH_PATTERNS:
        text = pattern.sub(lambda _m, r=repl: r, text)
    return text


def _strip_empty_code_blocks(lines: list[str]) -> list[str]:
    # Remove blocks that look like:
    # ```
    #
    # ```
    out: list[str] = []
    i = 0
    while i < len(lines):
        if lines[i].strip() == "```":
            j = i + 1
            while j < len(lines) and lines[j].strip() != "```":
                j += 1
            if j < len(lines) and all(l.strip() == "" for l in lines[i + 1 : j]):
                i = j + 1
                continue
        out.append(lines[i])
        i += 1
    return out


def _strip_public_header_blocks(lines: list[str]) -> list[str]:
    """Remove previously-added public masking header blocks.

    This makes the sanitizer idempotent even if the input was already sanitized.
    """
    out: list[str] = []
    i = 0
    while i < len(lines):
        if lines[i].lstrip().startswith("<!--"):
            j = i
            while j < len(lines):
                if "-->" in lines[j]:
                    break
                j += 1
            if j < len(lines):
                block = "\n".join(lines[i : j + 1])
                if "このファイルは公開向けにマスキング/整形済みです。" in block:
                    i = j + 1
                    continue
        out.append(lines[i])
        i += 1
    return out


def _strip_existing_turn_markers(lines: list[str]) -> list[str]:
    out: list[str] = []
    in_code_block = False

    for line in lines:
        stripped = line.strip()

        if stripped.startswith("```"):
            in_code_block = not in_code_block
            out.append(line)
            continue

        if not in_code_block and stripped in {"### User", "### GitHub Copilot", "---"}:
            continue

        out.append(line)

    return out


def _add_turn_markers(lines: list[str]) -> list[str]:
    out: list[str] = []
    in_code_block = False
    user_turn_count = 0

    for line in lines:
        stripped = line.strip()

        if stripped.startswith("```"):
            in_code_block = not in_code_block
            out.append(line)
            continue

        if not in_code_block and line.startswith("User:"):
            user_turn_count += 1
            if user_turn_count > 1:
                out.extend(["", "---", ""])  # turn separator
            out.extend(["### User", ""])  # marker
            out.append(line)
            continue

        if not in_code_block and line.startswith("GitHub Copilot:"):
            out.extend(["", "### GitHub Copilot", ""])  # marker
            out.append(line)
            continue

        out.append(line)

    return out


def sanitize_markdown(in_path: Path) -> str:
    raw = in_path.read_text(encoding="utf-8")
    lines = raw.splitlines()

    lines = _strip_public_header_blocks(lines)

    cleaned: list[str] = []
    for line in lines:
        if _is_noise_line(line):
            continue
        cleaned.append(_sanitize_text(line))

    cleaned = _strip_empty_code_blocks(cleaned)
    cleaned = _strip_existing_turn_markers(cleaned)
    cleaned = _add_turn_markers(cleaned)

    # Collapse excessive blank lines
    collapsed: list[str] = []
    blank_run = 0
    for line in cleaned:
        if line.strip() == "":
            blank_run += 1
            if blank_run <= 2:
                collapsed.append("")
            continue
        blank_run = 0
        collapsed.append(line)

    header = (
        "<!--\n"
        "このファイルは公開向けにマスキング/整形済みです。\n"
        "- 絶対パスや file:/// リンクは削除または一般化\n"
        "- Copilot内部ログ（Created todos/Starting/Made changes等）は削除\n"
        "-->\n\n"
    )
    return header + "\n".join(collapsed).rstrip() + "\n"


def main() -> None:
    # Preferred layout:
    #   docs/ai-logs/raw/*.md   -> docs/ai-logs/public/*.md
    # Fallback layout (current repo style):
    #   docs/ai-logs/*_raw.md   -> docs/ai-logs/*.md
    if RAW_DIR.exists():
        in_dir = RAW_DIR
        out_dir = PUBLIC_DIR
        out_dir.mkdir(parents=True, exist_ok=True)

        md_files = sorted(in_dir.glob("会話内容_*.md"))
        if not md_files:
            raise SystemExit("No raw markdown logs found (会話内容_*.md)")

        for in_path in md_files:
            out_path = out_dir / in_path.name
            out_path.write_text(sanitize_markdown(in_path), encoding="utf-8")
            print(f"wrote: {out_path}")
        return

    # Fallback: run in-place for *_raw.md
    if not BASE_DIR.exists():
        raise SystemExit(f"AI logs dir not found: {BASE_DIR}")

    md_files = sorted(BASE_DIR.glob("会話内容_*_raw.md"))
    if not md_files:
        raise SystemExit("No raw markdown logs found (会話内容_*_raw.md)")

    for in_path in md_files:
        stem = in_path.stem
        if stem.endswith("_raw"):
            out_name = stem.removesuffix("_raw") + in_path.suffix
        else:
            out_name = in_path.name
        out_path = BASE_DIR / out_name
        out_path.write_text(sanitize_markdown(in_path), encoding="utf-8")
        print(f"wrote: {out_path}")


if __name__ == "__main__":
    main()
