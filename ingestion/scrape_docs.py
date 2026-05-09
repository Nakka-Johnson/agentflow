"""
Fetches the Anthropic Claude documentation corpus.

Anthropic publishes a flattened, plain-text version of their entire
documentation site at /llms-full.txt, designed for use as LLM context
or as a corpus for retrieval systems. We use this directly rather than
scraping the HTML site (which is a React app and requires rendering).
"""

from pathlib import Path
import requests

CLAUDE_DOCS_URL = "https://docs.claude.com/llms-full.txt"

CORPUS_DIR = Path(__file__).parent / "corpus"
OUTPUT_FILE = CORPUS_DIR / "claude-docs.txt"


def scrape() -> None:
    CORPUS_DIR.mkdir(exist_ok=True)

    print(f"Fetching {CLAUDE_DOCS_URL}...")
    response = requests.get(CLAUDE_DOCS_URL, timeout=60)
    response.raise_for_status()

    text = response.text
    print(f"Got {len(text):,} characters")

    OUTPUT_FILE.write_text(text, encoding="utf-8")
    print(f"Wrote corpus to {OUTPUT_FILE}")


if __name__ == "__main__":
    scrape()