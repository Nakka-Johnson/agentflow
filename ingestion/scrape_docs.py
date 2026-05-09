"""
Scrapes the Spring Boot reference documentation single-page HTML
and saves it as plain text to corpus/spring-boot-reference.txt.
"""

from pathlib import Path
import requests
from bs4 import BeautifulSoup

SPRING_BOOT_REFERENCE_URL = (
    "https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/"
)

CORPUS_DIR = Path(__file__).parent / "corpus"
OUTPUT_FILE = CORPUS_DIR / "spring-boot-reference.txt"


def scrape() -> None:
    CORPUS_DIR.mkdir(exist_ok=True)

    print(f"Fetching {SPRING_BOOT_REFERENCE_URL}...")
    response = requests.get(SPRING_BOOT_REFERENCE_URL, timeout=60)
    response.raise_for_status()

    print(f"Got {len(response.content):,} bytes. Parsing...")
    soup = BeautifulSoup(response.content, "lxml")

    # Remove navigation, scripts, styles - we want body content only
    for tag in soup(["script", "style", "nav", "header", "footer"]):
        tag.decompose()

    # Extract text, preserving section structure via newlines around headings
    lines = []
    for element in soup.find_all(
        ["h1", "h2", "h3", "h4", "h5", "p", "li", "pre", "code"]
    ):
        text = element.get_text(separator=" ", strip=True)
        if not text:
            continue
        # Add markdown-ish heading markers so chunking can detect section boundaries
        if element.name.startswith("h"):
            level = int(element.name[1])
            lines.append("\n" + "#" * level + " " + text + "\n")
        else:
            lines.append(text)

    full_text = "\n".join(lines)

    OUTPUT_FILE.write_text(full_text, encoding="utf-8")
    print(f"Wrote {len(full_text):,} characters to {OUTPUT_FILE}")


if __name__ == "__main__":
    scrape()