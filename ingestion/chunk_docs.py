"""
Reads files in corpus/ and produces chunks.jsonl,
where each line is a JSON object with { id, text, source, section }.
"""

import json
import re
from pathlib import Path
from uuid import uuid4

CORPUS_DIR = Path(__file__).parent / "corpus"
OUTPUT_FILE = Path(__file__).parent / "chunks.jsonl"

TARGET_CHUNK_SIZE = 800   # characters
CHUNK_OVERLAP = 100       # characters of overlap between adjacent chunks
MIN_CHUNK_SIZE = 100      # discard chunks smaller than this


def split_into_sections(text: str) -> list[tuple[str, str]]:
    """
    Splits text on heading markers (lines starting with #).
    Returns a list of (heading, body) tuples.
    """
    pattern = re.compile(r"^(#+)\s+(.+)$", re.MULTILINE)
    matches = list(pattern.finditer(text))

    if not matches:
        return [("Document", text)]

    sections = []
    for i, match in enumerate(matches):
        heading = match.group(2).strip()
        body_start = match.end()
        body_end = matches[i + 1].start() if i + 1 < len(matches) else len(text)
        body = text[body_start:body_end].strip()
        if body:
            sections.append((heading, body))
    return sections


def split_into_chunks(text: str) -> list[str]:
    """
    Splits text into overlapping chunks of approximately TARGET_CHUNK_SIZE characters.
    """
    if len(text) <= TARGET_CHUNK_SIZE:
        return [text] if len(text) >= MIN_CHUNK_SIZE else []

    chunks = []
    start = 0
    while start < len(text):
        end = min(start + TARGET_CHUNK_SIZE, len(text))
        # Try to break on a sentence boundary
        if end < len(text):
            sentence_end = text.rfind(". ", start, end)
            if sentence_end > start + MIN_CHUNK_SIZE:
                end = sentence_end + 1
        chunk = text[start:end].strip()
        if len(chunk) >= MIN_CHUNK_SIZE:
            chunks.append(chunk)
        start = end - CHUNK_OVERLAP
        if start < 0:
            start = 0
        if end == len(text):
            break
    return chunks


def chunk_corpus() -> None:
    if not CORPUS_DIR.exists():
        raise SystemExit(f"Corpus directory not found: {CORPUS_DIR}")

    all_chunks = []
    for source_file in CORPUS_DIR.glob("*.txt"):
        print(f"Processing {source_file.name}...")
        text = source_file.read_text(encoding="utf-8")
        sections = split_into_sections(text)
        print(f"  Found {len(sections)} sections")

        for heading, body in sections:
            for chunk_text in split_into_chunks(body):
                all_chunks.append({
                    "id": str(uuid4()),
                    "text": chunk_text,
                    "source": source_file.name,
                    "section": heading,
                })

    print(f"Generated {len(all_chunks):,} chunks total")

    with OUTPUT_FILE.open("w", encoding="utf-8") as f:
        for chunk in all_chunks:
            f.write(json.dumps(chunk) + "\n")
    print(f"Wrote chunks to {OUTPUT_FILE}")


if __name__ == "__main__":
    chunk_corpus()