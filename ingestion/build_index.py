"""
Reads chunks.jsonl, embeds each chunk using a local sentence-transformers model,
and builds a FAISS index. Outputs faiss_index.bin and chunk_metadata.json
to the retrieval-service/ folder so the sidecar can load them at runtime.
"""

import json
from pathlib import Path
import numpy as np
import faiss
from sentence_transformers import SentenceTransformer
from tqdm import tqdm

INGESTION_DIR = Path(__file__).parent
RETRIEVAL_DIR = INGESTION_DIR.parent / "retrieval-service"
RETRIEVAL_DIR.mkdir(exist_ok=True)

CHUNKS_FILE = INGESTION_DIR / "chunks.jsonl"
INDEX_FILE = RETRIEVAL_DIR / "faiss_index.bin"
METADATA_FILE = RETRIEVAL_DIR / "chunk_metadata.json"

MODEL_NAME = "sentence-transformers/all-MiniLM-L6-v2"
EMBEDDING_DIM = 384


def build_index() -> None:
    if not CHUNKS_FILE.exists():
        raise SystemExit(f"Chunks file not found: {CHUNKS_FILE}. Run chunk_docs.py first.")

    print(f"Loading embedding model: {MODEL_NAME}")
    model = SentenceTransformer(MODEL_NAME)

    print(f"Reading chunks from {CHUNKS_FILE}")
    chunks = []
    with CHUNKS_FILE.open(encoding="utf-8") as f:
        for line in f:
            chunks.append(json.loads(line))
    print(f"  Loaded {len(chunks):,} chunks")

    texts = [chunk["text"] for chunk in chunks]

    print("Generating embeddings...")
    embeddings = model.encode(
        texts,
        batch_size=64,
        show_progress_bar=True,
        convert_to_numpy=True,
        normalize_embeddings=True,
    )
    print(f"  Embedding matrix shape: {embeddings.shape}")

    print("Building FAISS index (IndexFlatIP for cosine similarity)...")
    index = faiss.IndexFlatIP(EMBEDDING_DIM)
    index.add(embeddings.astype(np.float32))
    print(f"  Index size: {index.ntotal:,} vectors")

    print(f"Saving index to {INDEX_FILE}")
    faiss.write_index(index, str(INDEX_FILE))

    print(f"Saving metadata to {METADATA_FILE}")
    metadata = [
        {
            "id": chunk["id"],
            "source": chunk["source"],
            "section": chunk["section"],
            "text": chunk["text"],
        }
        for chunk in chunks
    ]
    METADATA_FILE.write_text(json.dumps(metadata, indent=2), encoding="utf-8")

    print("Done.")
    print(f"  Index: {INDEX_FILE} ({INDEX_FILE.stat().st_size / 1024 / 1024:.2f} MB)")
    print(f"  Metadata: {METADATA_FILE} ({METADATA_FILE.stat().st_size / 1024 / 1024:.2f} MB)")


if __name__ == "__main__":
    build_index()